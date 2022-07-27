package us.huseli.soundboard.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentHelpBinding

@AndroidEntryPoint
class HelpFragment : Fragment() {
    private lateinit var binding: FragmentHelpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val sections = getHelpSections()
        binding = FragmentHelpBinding.inflate(inflater, container, false)
        binding.cancel.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        binding.helpContent.adapter = HelpSectionAdapter(requireContext(), sections)
        return binding.root
    }

    private fun getHelpSections(): List<HelpSection> {
        return listOf(
            HelpSection(R.string.help_section_menu_selections, listOf(
                HelpItem(R.drawable.ic_add_sound, R.string.help_add_sounds_title, R.string.help_add_sounds_text),
                HelpItem(R.drawable.ic_reorder, R.string.help_reorder_title, R.string.help_reorder_text),
                HelpItem(R.drawable.ic_filter, R.string.help_filter_title, R.string.help_filter_text),
                HelpItem(R.drawable.ic_settings, R.string.help_settings_title, R.string.help_settings_text),
                HelpItem(R.drawable.ic_add_category, R.string.help_add_category_title),
                HelpItem(listOf(R.drawable.ic_zoom_in, R.drawable.ic_zoom_out),
                    R.string.help_zoom_title,
                    R.string.help_zoom_text),
                HelpItem(R.drawable.ic_undo, R.string.help_undo_title, R.string.help_undo_text),
                HelpItem(R.drawable.ic_redo, R.string.help_redo_title, R.string.help_redo_text)
            )),
            HelpSection(R.string.help_section_repress_mode, R.string.help_repress_mode_pretext, null, listOf(
                HelpItem(R.drawable.ic_repress_stop, R.string.help_repress_stop_title, R.string.help_repress_stop_text),
                HelpItem(R.drawable.ic_repress_restart,
                    R.string.help_repress_restart_title,
                    R.string.help_repress_restart_text),
                HelpItem(R.drawable.ic_repress_overlap,
                    R.string.help_repress_overlap_title,
                    R.string.help_repress_overlap_text),
                HelpItem(R.drawable.ic_pause, R.string.help_repress_pause_title, R.string.help_repress_pause_text)
            )),
            HelpSection(R.string.help_section_sound_selection,
                R.string.help_sound_selection_pretext,
                R.string.help_sound_selection_posttext,
                listOf(
                    HelpItem(R.drawable.ic_select_all, R.string.help_select_all_sounds_title),
                    HelpItem(R.drawable.ic_edit, R.string.help_edit_sounds_title),
                    HelpItem(R.drawable.ic_delete, R.string.help_delete_sounds_title),
                )),
            HelpSection(R.string.help_section_category_actions, listOf(
                HelpItem(R.drawable.ic_edit, R.string.help_edit_category_title, R.string.help_edit_category_text),
                HelpItem(R.drawable.ic_delete, R.string.help_delete_category_title, R.string.help_delete_category_text)
            )),
            HelpSection(R.string.help_section_why_oue, R.string.help_why_oue_pretext)
        )
    }


    data class HelpItem(val icons: List<Int>, val title: Int, val text: Int?) {
        constructor(icon: Int, title: Int, text: Int) : this(listOf(icon), title, text)
        constructor(icon: Int, title: Int) : this(listOf(icon), title, null)
    }

    data class HelpSection(val title: Int, val preText: Int?, val postText: Int?, val items: List<HelpItem>) {
        constructor(title: Int, items: List<HelpItem>) : this(title, null, null, items)
        constructor(title: Int, preText: Int) : this(title, preText, null, emptyList())
    }

    inner class HelpSectionAdapter(context: Context, private val sections: List<HelpSection>) :
        ArrayAdapter<HelpSection>(context, 0, sections) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_help_section, parent, false)

            val section = sections[position]

            view.findViewById<TextView>(R.id.title).text = getText(section.title)
            if (section.preText != null) view.findViewById<TextView>(R.id.preText).apply {
                text = getText(section.preText)
                visibility = View.VISIBLE
            }
            if (section.postText != null) view.findViewById<TextView>(R.id.postText).apply {
                text = getText(section.postText)
                visibility = View.VISIBLE
            }

            val itemListView = view.findViewById<LinearLayout>(R.id.items)
            itemListView.removeAllViews()

            section.items.forEach { item ->
                val itemView = layoutInflater.inflate(R.layout.item_help_item, itemListView, false)
                val imageListView = itemView.findViewById<LinearLayout>(R.id.icons)

                itemView.findViewById<TextView>(R.id.title).text = getText(item.title)
                if (item.text != null) itemView.findViewById<TextView>(R.id.text).apply {
                    text = getText(item.text)
                    visibility = View.VISIBLE
                }

                imageListView.removeAllViews()
                item.icons.mapNotNull { ResourcesCompat.getDrawable(context.resources, it, context.theme) }
                    .forEach { drawable ->
                        val imageView = ImageView(context)
                        imageView.setImageDrawable(drawable)
                        imageListView.addView(imageView)
                    }

                itemListView.addView(itemView)
            }

            return view
        }
    }

}