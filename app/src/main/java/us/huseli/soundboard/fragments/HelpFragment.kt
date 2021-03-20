package us.huseli.soundboard.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.tiagohm.markdownview.Utils
import br.tiagohm.markdownview.css.InternalStyleSheet
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentHelpBinding

class HelpFragment : Fragment(), View.OnClickListener {
    private var binding: FragmentHelpBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val css = StyleSheet(requireContext())
        val isNightMode =
            resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        @Suppress("DEPRECATION") val locale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales.get(0) else resources.configuration.locale
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

    class StyleSheet(context: Context) : InternalStyleSheet() {
        init {
            val attr = TypedValue()
            context.theme.resolveAttribute(R.attr.colorSurface, attr, true)
            val backgroundColor =
                if (attr.type >= TypedValue.TYPE_FIRST_COLOR_INT && attr.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    String.format("%06X", 0xFFFFFF.and(attr.data))
                } else null
            context.theme.resolveAttribute(R.attr.colorOnPrimary, attr, true)
            val foregroundColor =
                if (attr.type >= TypedValue.TYPE_FIRST_COLOR_INT && attr.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    String.format("%06X", 0xFFFFFF.and(attr.data))
                } else null
            if (foregroundColor != null && backgroundColor != null) {
                addRule("body", "color: #$foregroundColor")
                addRule("body", "background-color: #$backgroundColor")
            }
            addRule("img", "margin-right: 1rem")
        }
    }
}