package com.kuafor.app.data.repository

/** Kullanıcıya gösterilebilir (Türkçe) iş hatalarının ortak tabanı. */
open class KuaforException(message: String) : Exception(message)

class NotSignedInException : KuaforException("Randevu almak için giriş yapmanız gerekiyor.")

class SlotNotFoundException : KuaforException("Seçilen saat bulunamadı.")

class SlotAlreadyBookedException : KuaforException("Seçilen saat az önce doldu. Lütfen başka bir saat seçin.")

class ServiceNotFoundException : KuaforException("Seçilen hizmet bulunamadı.")

/** Slot başlangıcı geçmişte kalmış (sunucu PAST_SLOT ile reddetti). */
class SlotInPastException : KuaforException("Seçilen saat geçmişte kaldı. Lütfen başka bir saat seçin.")

/** Sunucu trigger'ı randevuyu reddetti ve nedeni bilinmiyor/eşleşmiyor. */
class AppointmentRejectedException : KuaforException("Randevu oluşturulamadı. Lütfen tekrar deneyin.")
