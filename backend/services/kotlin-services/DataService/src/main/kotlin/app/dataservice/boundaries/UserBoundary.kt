package app.dataservice.boundaries

import java.util.Date

class UserBoundary(
    var id: String?,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var mobile: String?,
    var username: String?,
    var password: String?,
    var createdAt: Date?,
    var updatedAt: Date?
) {
    constructor() : this(null, null, null, null, null, null, null, null, null)

    constructor(userEntity: UserEntity): this(){
        this.id = userEntity.id
        this.firstName = userEntity.firstName
        this.lastName = userEntity.lastName
        this.email = userEntity.email
        this.mobile = userEntity.mobile
        this.username = userEntity.username
        this.password = userEntity.password
        this.createdAt = userEntity.createdAt
        this.updatedAt = userEntity.updatedAt
    }

    fun toEntity(): UserEntity{
        val userEntity = UserEntity()

        userEntity.id = id
        userEntity.firstName = firstName
        userEntity.lastName = lastName
        userEntity.email = email
        userEntity.mobile = mobile
        userEntity.username = username
        userEntity.password = password
        userEntity.createdAt = createdAt
        userEntity.updatedAt = updatedAt

        return userEntity
    }

    override fun toString(): String {
        return "UserEntity(" +
                "id=$id," +
                " firstName=$firstName," +
                " lastName=$lastName," +
                " email=$email," +
                " mobile=$mobile," +
                " username=$username," +
                " password=$password," +
                " createdAt=$createdAt," +
                " updatedAt=$updatedAt)"
    }
}