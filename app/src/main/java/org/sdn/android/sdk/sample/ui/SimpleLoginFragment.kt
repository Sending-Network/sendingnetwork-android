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

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.util.Log

import kotlinx.coroutines.launch

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okio.ByteString.Companion.decodeHex
import org.sdn.android.sdk.api.auth.data.EdgeNodeConnectionConfig
import org.sdn.android.sdk.sample.R
import org.sdn.android.sdk.sample.SampleApp
import org.sdn.android.sdk.sample.SessionHolder
import org.sdn.android.sdk.sample.databinding.FragmentLoginBinding
import org.sdn.android.sdk.server.RadixService
import org.web3j.crypto.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber

data class SignRequest(val message: String)
data class SignResponse(val signature: String)

interface SignService {
    @POST("/_api/appservice/sign")
    suspend fun signMessage(@Body signRequest: SignRequest) : SignResponse
}

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

    private fun launchAuthProcess() {
        val nodeUrl = views.nodeField.text.toString().trim()
        val address  = views.addressField.text.toString().trim()
        val privateKey   = views.privateField.text.toString().trim()

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

        viewLifecycleOwner.lifecycleScope.launch {

            val ecKeyPair: ECKeyPair = ECKeyPair.create(privateKey.decodeHex().toByteArray())
            val authService = SampleApp.getSDNClient(requireContext()).authenticationService()
            val sp = requireContext().getSharedPreferences("device_data", MODE_PRIVATE)
            var deviceIdKey = "device_id_$address"
            var deviceId = ""
            try {
                val fedInfo = authService.getFedInfo(edgeNodeConnectionConfig)
                edgeNodeConnectionConfig.peerId = fedInfo.peer
                deviceIdKey = "device_id_${fedInfo.peer}_$address"
                deviceId = sp.getString(deviceIdKey, "") ?: ""

                val loginDidMsg = authService.didPreLogin(edgeNodeConnectionConfig, address, deviceId)
                if (loginDidMsg.message is String) {
                    Log.d("loginLoginDidMsg", loginDidMsg.message)
                }

                val token = signMessage(ecKeyPair, loginDidMsg.message)
                if (token is String) {
                    Log.d("loginLoginDidMsg:token", token)
                }

                val appToken = signWithServerDeveloperKey((loginDidMsg.message))
                authService.didLogin(edgeNodeConnectionConfig, address,
                    loginDidMsg.did, loginDidMsg.randomServer, loginDidMsg.updated, token, appToken)
            } catch (failure: Throwable) {
                Timber.tag("login").e("login fail: ${failure.message}")
//                var msg = ${failure.message}
//                Log.d("loginFailure", msg)
                Toast.makeText(requireContext(), "Failure: $failure", Toast.LENGTH_SHORT).show()
                null
            }?.let {
                val retDeviceId = it.sessionParams.deviceId
                if (retDeviceId != deviceId) {
                    Timber.tag("login").i("get new device id: $retDeviceId")
                    sp.edit().putString(deviceIdKey, retDeviceId).apply()
                }
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

    // replace with your own implementation
    private suspend fun signWithServerDeveloperKey(message: String): String {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://rewards.sending.network")
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .client(OkHttpClient())
            .build()
        val signService = retrofit.create(SignService::class.java)
        val signResp = signService.signMessage(SignRequest(message))
        return signResp.signature
    }
}
