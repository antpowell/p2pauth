package com.anthony_powell.p2pauth

import java.util.*

class CodeNameGenerator {

    private val COLOR = arrayOf(
            "Red",

            "Orange",

            "Yellow",

            "Green",

            "Blue",

            "Indigo",

            "Violet",

            "Purple",

            "Lavender",

            "Fuchsia",

            "Plum",

            "Orchid",

            "Magenta"
    )
    private val TREATS = arrayOf(
            "Alpha",

            "Beta",

            "Cupcake",

            "Donut",

            "Eclair",

            "Froyo",

            "Gingerbread",

            "Honeycomb",

            "Ice Cream Sandwich",

            "Jellybean",

            "Kit Kat",

            "Lollipop",

            "Marshmallow",

            "Nougat"
    )

    private val generator = Random()

    private constructor()

    companion object {
        fun generate():String {
            val codeNameGenerator = CodeNameGenerator()
            val color  = codeNameGenerator.COLOR[codeNameGenerator.generator.nextInt(codeNameGenerator.COLOR.size)]
            val treats  = codeNameGenerator.TREATS[codeNameGenerator.generator.nextInt(codeNameGenerator.TREATS.size)]
            return "$color $treats"
        }
    }

    fun generate():String{
        val color  = COLOR[generator.nextInt(COLOR.size)]
        val treats  = TREATS[generator.nextInt(TREATS.size)]
        return "$color $treats"
    }
}