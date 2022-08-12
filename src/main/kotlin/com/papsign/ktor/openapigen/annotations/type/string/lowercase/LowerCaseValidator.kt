package com.papsign.ktor.openapigen.annotations.type.string.lowercase

import com.papsign.ktor.openapigen.getKType
import com.papsign.ktor.openapigen.validation.Validator
import com.papsign.ktor.openapigen.annotations.type.SingleTypeValidator
import java.util.Locale

object LowerCaseValidator : SingleTypeValidator<LowerCase>(getKType<String>(), { LowerCaseValidator }), Validator {
    override fun <T> validate(subject: T?): T? {
        @Suppress("UNCHECKED_CAST")
        return (subject as String?)?.lowercase(Locale.getDefault()) as T?
    }
}
