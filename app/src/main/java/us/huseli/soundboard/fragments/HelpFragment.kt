package us.huseli.soundboard.fragments

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.tiagohm.markdownview.Utils
import br.tiagohm.markdownview.css.InternalStyleSheet
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentHelpBinding
import us.huseli.soundboard.helpers.ColorHelper
import javax.inject.Inject

@AndroidEntryPoint
class HelpFragment : Fragment(), View.OnClickListener {
    /**
     * https://github.com/tiagohm/MarkdownView
     * https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet
     */
    @Inject
    lateinit var colorHelper: ColorHelper
    private var binding: FragmentHelpBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val css = StyleSheet()
        val isNightMode =
            resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        @Suppress("DEPRECATION")
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            resources.configuration.locales.get(0) else resources.configuration.locale

        val inputStream = try {
            resources.assets.open("${locale!!.language}/help.md")
        } catch (e: Exception) {
            resources.assets.open("help.md")
        }

        val md = Utils.getStringFromInputStream(inputStream).let {
            if (isNightMode) it.replace("android_asset", "android_asset/night") else it
        }

        return FragmentHelpBinding.inflate(inflater, container, false).run {
            binding = this
            cancel.setOnClickListener(this@HelpFragment)
            markdown.addStyleSheet(css)
            markdown.loadMarkdown(md)
            root
        }
    }

    override fun onClick(view: View?) {
        if (view == binding?.cancel) {
            parentFragmentManager.popBackStackImmediate()
        }
    }

    inner class StyleSheet : InternalStyleSheet() {
        init {
            val backgroundColor = colorHelper.getColorStringFromAttr(R.attr.colorSurface, requireContext().theme)
            val foregroundColor = colorHelper.getColorStringFromAttr(R.attr.colorOnSurface, requireContext().theme)
            val listDividerColor = colorHelper.getColorStringFromAttr(R.attr.colorDivider, requireContext().theme)
            val scrollupBackgroundColor =
                colorHelper.getColorStringFromAttr(R.attr.colorPrimary, requireContext().theme)
            // val listDividerColor = colorHelper.getColorStringFromResId(R.color.grey_800)
            if (foregroundColor != null && backgroundColor != null) {
                addRule("body", "color: #$foregroundColor")
                addRule("body", "background-color: #$backgroundColor")
            }
            if (scrollupBackgroundColor != null) addRule(".scrollup", "background-color: #$scrollupBackgroundColor")
            addRule("img", "margin-right: 1rem")
            addRule("hr", "border-top: 0", "margin: 2rem 0")
            if (listDividerColor != null) addRule("hr", "border-color: #$listDividerColor")
        }
    }
}