package app.dataservice.boundaries

import app.dataservice.entities.UserEntity
import java.time.LocalDateTime

class UserBoundary(
    var id: String?,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var password: String?,
    var mobile: String?,
    var username: String?,
    var createdAt: LocalDateTime?,
    var updatedAt: LocalDateTime?
) {
    constructor() : this(null, null, null, null, null, null, null, null, null)

    constructor(userEntity: UserEntity): this(){
        this.id = userEntity.id
        this.firstName = userEntity.firstName
        this.lastName = userEntity.lastName
        this.email = userEntity.email
        this.password = userEntity.password
        this.mobile = userEntity.mobile
        this.username = userEntity.username
        this.createdAt = userEntity.createdAt
        this.updatedAt = userEntity.updatedAt
    }

    fun toEntity(): UserEntity{
        val userEntity = UserEntity()

        userEntity.id = id
        userEntity.firstName = firstName
        userEntity.lastName = lastName
        userEntity.email = email
        userEntity.password = password
        userEntity.mobile = mobile
        userEntity.username = username
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
                " password=$password," +
                " mobile=$mobile," +
                " username=$username," +
                " createdAt=$createdAt," +
                " updatedAt=$updatedAt)"
    }
}