package bo.saludencasa.features.requests.domain.model

enum class RequestStatus {
    PUBLISHED,
    NEGOTIATING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    ;

    // RF-07.6: the patient cancels while the request has not been taken. Once an
    // offer is accepted the request is a contract, and the atomic operation
    // RF-08.5 owns every transition out of it, not a direct client write.
    val isCancellable: Boolean
        get() = this == PUBLISHED || this == NEGOTIATING
}
