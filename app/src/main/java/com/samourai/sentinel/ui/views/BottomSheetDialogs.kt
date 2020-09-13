package com.samourai.sentinel.ui.views

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.samourai.sentinel.R


/**
 * sentinel-android
 *
 * @author Sarath
 */
open class GenericBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)


}

open class GenericAlertBottomSheet(private val onViewReady: (GenericBottomSheet) -> Unit?) : GenericBottomSheet() {

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady(this)
    }

}

class InputBottomSheet(private val label: String, onViewReady: (GenericBottomSheet) -> Unit) : GenericAlertBottomSheet(onViewReady) {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet, container)
        view.findViewById<TextView>(R.id.dialogTitle).text = label
        val inputContent = inflater.inflate(R.layout.content_bottom_sheet_input, null)
        val content = view.findViewById<FrameLayout>(R.id.contentContainer)
        content.addView(inputContent)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val inputField = view.findViewById<TextInputEditText>(R.id.bottomSheetInputField)
        inputField.requestFocus()
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    override fun onDismiss(dialog: DialogInterface) {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        super.onDismiss(dialog)
    }
}

class ConfirmBottomSheet(private val label: String, onViewReady: (GenericBottomSheet) -> Unit) : GenericAlertBottomSheet(onViewReady) {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet, container)
        view.findViewById<TextView>(R.id.dialogTitle).text = label
        val inputContent = inflater.inflate(R.layout.content_bottom_sheet_confirm, null)
        val content = view.findViewById<FrameLayout>(R.id.contentContainer)
        content.addView(inputContent)
        return view
    }

}

fun AppCompatActivity.alertWithInput(label: String,
                                     buttonLabel: String = "Confirm",
                                     value: String = "",
                                     labelEditText: String = "",
                                     isCancelable: Boolean = true,
                                     maxLen: Int = 34,
                                     onConfirm: (String) -> Unit): GenericBottomSheet {
    val bottomSheet = InputBottomSheet(label, onViewReady = {
        val view = it.view
        view?.findViewById<MaterialButton>(R.id.bottomSheetConfirmPositiveBtn)?.text = buttonLabel
        view?.findViewById<TextInputLayout>(R.id.bottomSheetInputFieldLayout)?.hint = labelEditText
        val textInput = view?.findViewById<TextInputEditText>(R.id.bottomSheetInputField);
        textInput?.setText(value)
        textInput?.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLen))
        view?.findViewById<MaterialButton>(R.id.bottomSheetConfirmPositiveBtn)?.setOnClickListener { _ ->
            val text = textInput?.text.toString()
            onConfirm.invoke(text)
            it.dismiss()
        }
    })
    bottomSheet.isCancelable = isCancelable
    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    return bottomSheet
}


fun AppCompatActivity.confirm(label: String = "",
                              positiveText: String = "Yes",
                              message: String = "",
                              negativeText: String = "No",
                              isCancelable: Boolean = true,
                              onConfirm: (Boolean) -> Unit): GenericBottomSheet {
    val bottomSheet = ConfirmBottomSheet(label, onViewReady = { bottomSheet ->
        val view = bottomSheet.view
        val positiveButton = view?.findViewById<MaterialButton>(R.id.bottomSheetConfirmPositiveBtn);
        val negativeButton = view?.findViewById<MaterialButton>(R.id.bottomSheetConfirmNegativeBtn);
        view?.findViewById<TextView>(R.id.bottomSheetConfirmMessage)?.text = message;

        positiveButton?.text = positiveText
        negativeButton?.text = negativeText

        negativeButton?.setOnClickListener {
            onConfirm(false)
            bottomSheet.dismiss()
        }
        positiveButton?.setOnClickListener {
            onConfirm(true)
            bottomSheet.dismiss()
        }
    })
    bottomSheet.isCancelable = isCancelable
    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    return bottomSheet
}

