package us.huseli.soundboard.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentStatusDialogBinding

open class StatusDialogFragment : DialogFragment() {
    private val errorMessages = mutableListOf<String>()
    private val messages = mutableListOf<Message>()

    private var onPositiveButton: DialogInterface.OnClickListener? = null
    private var title: String? = null

    private lateinit var binding: FragmentStatusDialogBinding

    val messageCount: Int
        get() = messages.size

    fun addException(e: Exception): StatusDialogFragment {
        e.message?.let { errorMessages.add(it) }
        return this
    }

    fun addMessage(status: Status, message: String): StatusDialogFragment {
        messages.add(Message(status, message))
        return this
    }

    fun setOnPositiveButton(callback: () -> Unit): StatusDialogFragment {
        onPositiveButton = DialogInterface.OnClickListener { _, _ ->
            dismiss()
            callback.invoke()
        }
        return this
    }

    fun setTitle(value: String): StatusDialogFragment {
        title = value
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentStatusDialogBinding.inflate(layoutInflater)

        savedInstanceState?.getString(ARG_TITLE)?.let { title = it }
        savedInstanceState?.getStringArray(ARG_ERROR_MESSAGES)?.let { errorMessages.addAll(it) }
        savedInstanceState?.getParcelableArrayList<Message>(ARG_MESSAGES)?.let { messages.addAll(it) }

        binding.messages.adapter = MessageAdapter(requireContext(), messages)

        if (errorMessages.size > 0) {
            binding.errorMessageHeader.visibility = View.VISIBLE
            binding.errorMessageList.visibility = View.VISIBLE
            binding.errorMessageList.adapter =
                ArrayAdapter(requireContext(), R.layout.simple_list_item, errorMessages)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.ok, onPositiveButton)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARG_TITLE, title)
        outState.putParcelableArrayList(ARG_MESSAGES, ArrayList<Message>(messages))
        outState.putStringArray(ARG_ERROR_MESSAGES, errorMessages.toTypedArray())
        super.onSaveInstanceState(outState)
    }


    enum class Status { SUCCESS, ERROR, WARNING }


    data class Message(val status: Status, val text: String) : Parcelable {
        constructor(parcel: Parcel) : this(Status.valueOf(parcel.readString()!!), parcel.readString()!!)

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.writeString(status.name)
            dest?.writeString(text)
        }

        companion object CREATOR : Parcelable.Creator<Message> {
            override fun createFromParcel(parcel: Parcel) = Message(parcel)
            override fun newArray(size: Int): Array<Message?> = arrayOfNulls(size)
        }
    }


    inner class MessageAdapter(context: Context, val messages: List<Message>) :
        ArrayAdapter<Message>(context, 0, messages) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_status_dialog_message, parent, false)
            val message = messages[position]
            val iconView = view.findViewById<ImageView>(R.id.messageIcon)

            when (message.status) {
                Status.SUCCESS -> {
                    iconView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                        R.drawable.ic_check,
                        context.theme))
                    iconView.drawable.setTint(ResourcesCompat.getColor(resources, R.color.green_700, context.theme))
                }
                Status.ERROR -> {
                    iconView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                        R.drawable.ic_error,
                        context.theme))
                    iconView.drawable.setTint(ResourcesCompat.getColor(resources, R.color.red_700, context.theme))
                }
                Status.WARNING -> {
                    iconView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                        R.drawable.ic_warning,
                        context.theme))
                    iconView.drawable.setTint(ResourcesCompat.getColor(resources, R.color.yellow_700, context.theme))
                }
            }

            view.findViewById<TextView>(R.id.messageText).text = message.text

            return view
        }
    }


    companion object {
        const val ARG_TITLE = "title"
        const val ARG_MESSAGES = "messages"
        const val ARG_ERROR_MESSAGES = "errorMessages"
    }
}