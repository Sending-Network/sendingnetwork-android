package org.sdn.android.sdk.sample.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.sdn.android.sdk.sample.R
import org.sdn.android.sdk.sample.databinding.FragmentDlgPasswordBinding

class PasswordDialogFragment : DialogFragment() {

    private var _views: FragmentDlgPasswordBinding? = null
    private val views get() = _views!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onStart() {
        val params = dialog!!.window!!.attributes
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _views = FragmentDlgPasswordBinding.inflate(inflater, container, false)
        return views.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val inputPassword: EditText = view.findViewById(R.id.input_password)
        inputPassword.requestFocus()
        dialog!!.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        super.onViewCreated(view, savedInstanceState)

        val requestKey = arguments?.getString("requestKey") ?: ""
        views.btnOk.setOnClickListener {
            val password = views.inputPassword.text.toString()
            parentFragmentManager.setFragmentResult(requestKey, bundleOf(Pair("password", password)))
            dismiss()
        }
        views.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance(requestKey: String): PasswordDialogFragment {
            // Supply num input as an argument.
            val args = Bundle().also { it.putString("requestKey", requestKey) }
            return PasswordDialogFragment().also { it.arguments = args }
        }
    }
}