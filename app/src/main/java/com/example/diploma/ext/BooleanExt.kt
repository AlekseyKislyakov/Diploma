package com.example.diploma.ext

fun Boolean.simplify() : String {
    return if(this) "1" else "0"
}