package us.huseli.soundboard.helpers

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import us.huseli.soundboard.R

class LongSummaryPreferenceCategory @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    PreferenceCategory(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(android.R.id.summary) as? TextView)?.apply {
            isSingleLine = false
            maxLines = 5
        }
        (holder.findViewById(android.R.id.title) as? TextView)?.apply {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnBackground, typedValue, true)
            setTextColor(typedValue.data)
            textSize = 16.0.toFloat()
            typeface = Typeface.DEFAULT
        }
    }
}