/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sdn.android.sdk.sample.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okio.ByteString.Companion.decodeHex
import org.sdn.android.sdk.api.auth.data.EdgeNodeConnectionConfig
import org.sdn.android.sdk.sample.R
import org.sdn.android.sdk.sample.SampleApp
import org.sdn.android.sdk.sample.SessionHolder
import org.sdn.android.sdk.sample.databinding.FragmentLoginBinding
import org.sdn.android.sdk.server.RadixService
import org.web3j.crypto.*
import timber.log.Timber


class SimpleLoginFragment : Fragment() {

    private var _views: FragmentLoginBinding? = null
    private val views get() = _views!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _views = FragmentLoginBinding.inflate(inflater, container, false)
        return views.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.loginButton.setOnClickListener {
            launchAuthProcess()
        }
        views.startService.setOnClickListener{
            val intent = Intent(activity, RadixService::class.java)
            activity?.startService(intent)
        }
        views.stopService.setOnClickListener{
            val intent = Intent(activity, RadixService::class.java)
            activity?.stopService(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun launchAuthProcess() {
        val nodeUrl = views.nodeField.text.toString().trim()
        // First, create a node config
        // Be aware than it can throw if you don't give valid info
        val edgeNodeConnectionConfig = try {
            EdgeNodeConnectionConfig
                .Builder()
                .withNodeUri(Uri.parse(nodeUrl))
                .build()
        } catch (failure: Throwable) {
            Toast.makeText(requireContext(), "Home server is not valid", Toast.LENGTH_SHORT).show()
            return
        }
        // Then you can retrieve the authentication service.
        // Here we use the direct authentication, but you get LoginWizard and RegistrationWizard for more advanced feature
        //
        viewLifecycleOwner.lifecycleScope.launch {
            val address = "a4a43324220196914c6E97c032da79038deDa6A1"
            val privateKey = "d91a3ef7a542e7cf5f7146f6cafb7b6f84381ccb93e1bf505ba48278de1662f3"
            val ecKeyPair: ECKeyPair = ECKeyPair.create(privateKey.decodeHex().toByteArray())
            val authService = SampleApp.getSDNClient(requireContext()).authenticationService()
            try {
                val loginDidMsg = authService.didPreLogin(edgeNodeConnectionConfig, address)
                val token = signMessage(ecKeyPair, loginDidMsg.message)
                authService.didLogin(edgeNodeConnectionConfig,
                    loginDidMsg.did, loginDidMsg.randomServer, loginDidMsg.updated, token)
            } catch (failure: Throwable) {
                Timber.tag("login").e("login fail: ${failure.message}")
                Toast.makeText(requireContext(), "Failure: $failure", Toast.LENGTH_SHORT).show()
                null
            }?.let {
                SessionHolder.currentSession = it
                it.open()
                it.syncService().startSync(true)
                displayRoomList()
            }

        }
    }

    private fun displayRoomList() {
        val fragment = RoomListFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment).commit()
    }

    private fun signMessage(keyPair: ECKeyPair, message: String): String {
        val ethSignature = Sign.signPrefixedMessage(message.toByteArray(), keyPair)
        val sig = ethSignature.r + ethSignature.s + ethSignature.v
        return sig.toHex()
    }

    private fun ByteArray.toHex(prefix: String = "", upperCase: Boolean = false): String {
        return if (upperCase) {
            prefix + joinToString(separator = "") { eachByte -> "%02X".format(eachByte) }
        } else {
            prefix + joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        }
    }

}
