package com.locus.olympiad.ui.onboarding

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.locus.olympiad.databinding.DialogOnboardingBinding

class OnboardingDialog() : DialogFragment() {

    var onHandleChosen: ((String) -> Unit)? = null

    constructor(onHandleChosen: (String) -> Unit) : this() {
        this.onHandleChosen = onHandleChosen
    }

    private var _binding: DialogOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        isCancelable = false
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveProfile.setOnClickListener {
            val handle = binding.etUsername.text?.toString()?.trim().orEmpty()
            val chosenName = if (handle.isNotBlank()) handle else "Gauss"
            onHandleChosen?.invoke(chosenName)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "OnboardingDialog"

        fun newInstance(onHandleChosen: (String) -> Unit): OnboardingDialog {
            return OnboardingDialog(onHandleChosen)
        }
    }
}
