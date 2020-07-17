package us.huseli.soundboard_kotlin

import petrov.kristiyan.colorpicker.ColorPicker.OnChooseColorListener

class test {
    var l = object : OnChooseColorListener {
        override fun onChooseColor(position: Int, color: Int) {}
        override fun onCancel() {}
    }
}