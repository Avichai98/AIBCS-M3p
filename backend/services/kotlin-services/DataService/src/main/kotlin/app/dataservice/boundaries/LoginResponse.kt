package app.dataservice.boundaries

class LoginResponse(
    val user: UserBoundary?,
    val token: String?
) {
    constructor() : this(null, null)

    override fun toString(): String {
        return "LoginResponse(user=$user, token=$token)"
    }
}