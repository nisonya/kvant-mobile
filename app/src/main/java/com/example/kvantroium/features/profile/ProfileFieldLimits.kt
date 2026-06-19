package com.example.kvantroium.features.profile

object ProfileFieldLimits {
    const val NAME_MAX = 45
    const val PATRONYMIC_MAX = 45
    const val CONTACT_MAX = 20
    const val SIZE_MAX = 10
    const val EDUCATION_MAX = 30
    const val PASSWORD_MIN = 6

    val EDUCATION_OPTIONS = listOf("СПО", "Высшее", "Высшее педагогическое")
    val GENDER_OPTIONS = listOf("Мужской", "Женский")

    private val PHONE_REGEX = Regex("""^((8|\+7)[\- ]?)?(\(?\d{3}\)?[\- ]?)?[\d\- ]{7,10}$""")

    fun validateContact(contact: String): String? {
        val trimmed = contact.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > CONTACT_MAX) return "Контактный номер слишком длинный (макс. $CONTACT_MAX)"
        if (!PHONE_REGEX.matches(trimmed)) return "Некорректный номер телефона"
        return null
    }

    fun validateProfileDraft(draft: ProfileEditDraft): String? {
        if (draft.secondName.trim().isEmpty()) return "Укажите фамилию"
        if (draft.firstName.trim().isEmpty()) return "Укажите имя"
        if (draft.dateOfBirthIso.isBlank()) return "Укажите дату рождения"
        if (draft.secondName.length > NAME_MAX) return "Фамилия слишком длинная (макс. $NAME_MAX)"
        if (draft.firstName.length > NAME_MAX) return "Имя слишком длинное (макс. $NAME_MAX)"
        if (draft.patronymic.length > PATRONYMIC_MAX) return "Отчество слишком длинное (макс. $PATRONYMIC_MAX)"
        validateContact(draft.contact)?.let { return it }
        if (draft.size.length > SIZE_MAX) return "Размер одежды слишком длинный (макс. $SIZE_MAX)"
        if (draft.education.length > EDUCATION_MAX) return "Образование слишком длинное (макс. $EDUCATION_MAX)"
        if (draft.gender.isNotBlank() && draft.gender !in GENDER_OPTIONS) return "Выберите пол из списка"
        if (draft.education.isNotBlank() && draft.education !in EDUCATION_OPTIONS) return "Выберите образование из списка"
        return null
    }

    fun validatePasswordChange(oldPassword: String, newPassword: String, confirmPassword: String): String? {
        if (oldPassword.isBlank()) return "Введите текущий пароль"
        if (newPassword.length < PASSWORD_MIN) return "Новый пароль слишком короткий (мин. $PASSWORD_MIN символов)"
        if (newPassword != confirmPassword) return "Пароли не совпадают"
        return null
    }
}

fun genderLabelForForm(raw: String?): String {
    return when (raw?.trim()?.lowercase().orEmpty()) {
        "м", "m", "male", "мужской" -> "Мужской"
        "ж", "f", "female", "женский" -> "Женский"
        else -> raw?.trim().orEmpty()
    }
}
