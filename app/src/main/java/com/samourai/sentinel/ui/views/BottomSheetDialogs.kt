package com.samourai.sentinel.ui.views

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
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

/**
 * Base BottomSheet class for the whole application
 * Custom themes with rounded corners are applied
 */
open class GenericBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.AppTheme_BottomSheet_Theme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        theme
    )

}

/**
 * GenericAlertBottomSheet is for alert style BottomSheet
 * This will help to make Alert typ BottomSheets
 * @see InputBottomSheet
 * @see ConfirmBottomSheet etc...
 */
open class GenericAlertBottomSheet(private val onViewReady: (GenericBottomSheet) -> Unit?) : GenericBottomSheet() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady(this)
    }

}

/**
 * Shows BottomSheet with Input field
 * It will immediately focus input field for better UX
 */
class InputBottomSheet(private val label: String, onViewReady: (GenericBottomSheet) -> Unit) : GenericAlertBottomSheet(
    onViewReady
) {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_bottom_sheet, container)
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

/**
 * Shows Confirmation style Bottomsheet
 * @param label
 * @param onViewReady
 */
class ConfirmBottomSheet(private val label: String, onViewReady: (GenericBottomSheet) -> Unit) : GenericAlertBottomSheet(
    onViewReady
) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_bottom_sheet, container)
        view.findViewById<TextView>(R.id.dialogTitle).text = label
        val inputContent = inflater.inflate(R.layout.content_bottom_sheet_confirm, null)
        val content = view.findViewById<FrameLayout>(R.id.contentContainer)
        content.addView(inputContent)
        return view
    }

}

/**
 * Shows BottomSheet with radio and checkbox
 * @param buttonLabel Confirmation button label
 * @param optionsChosen Default chosen option for checkbox
 */
fun AppCompatActivity.alertWithInput(
    label: String,
    buttonLabel: String = "Confirm",
    value: String = "",
    labelEditText: String = "",
    maskInput: Boolean = false,
    isCancelable: Boolean = true,
    maxLen: Int = 34,
    onConfirm: (String) -> Unit
): GenericBottomSheet {
    val bottomSheet = InputBottomSheet(label, onViewReady = {
        val view = it.view
        view?.findViewById<MaterialButton>(R.id.bottomSheetConfirmPositiveBtn)?.text = buttonLabel
        view?.findViewById<TextInputLayout>(R.id.bottomSheetInputFieldLayout)?.hint = labelEditText
        val textInput = view?.findViewById<TextInputEditText>(R.id.bottomSheetInputField);
        textInput?.setText(value)
        if(maskInput){
            textInput?.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            textInput?.transformationMethod = PasswordTransformationMethod.getInstance();
        }
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


fun AppCompatActivity.confirm(
    label: String = "",
    positiveText: String = "Yes",
    message: String = "",
    negativeText: String = "No",
    isCancelable: Boolean = true,
    onConfirm: (Boolean) -> Unit
): GenericBottomSheet {
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
