package ru.eleventh.svmd.exceptions

class TransformException(val errors: List<String> = emptyList()): RuntimeException()