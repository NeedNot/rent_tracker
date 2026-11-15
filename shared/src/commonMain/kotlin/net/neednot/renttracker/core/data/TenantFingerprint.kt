package net.neednot.renttracker.core.data

suspend fun PendingPaymentDao.tenantFingerprint(tenantId: String): String =
    tenantFingerprintParts(tenantId).joinToString(separator = ",")
