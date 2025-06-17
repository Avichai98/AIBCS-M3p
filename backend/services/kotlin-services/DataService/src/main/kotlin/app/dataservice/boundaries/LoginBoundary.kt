package app.dataservice.boundaries

class LoginBoundary(
    var email: String?,
    var password: String?
) {
    constructor() : this(null, null)

    override fun toString(): String {
        return "LoginBoundary(email='$email', password='$password')"
    }
}