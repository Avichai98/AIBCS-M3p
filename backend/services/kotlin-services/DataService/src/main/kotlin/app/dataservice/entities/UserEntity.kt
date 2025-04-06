package app.dataservice.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date

@Document (collection = "users")
class UserEntity (
    @Id var id: String?,
    var firstName: String?,
    var lastName: String?,
    var email: String?,
    var mobile: String?,
    var username: String?,
    var createdAt: Date?,
    var updatedAt: Date?
){
    constructor() : this(null, null, null, null, null, null, null, null)

    override fun toString(): String {
        return "UserEntity(" +
                "id=$id," +
                " firstName=$firstName," +
                " lastName=$lastName," +
                " email=$email," +
                " mobile=$mobile," +
                " username=$username," +
                " createdAt=$createdAt," +
                " updatedAt=$updatedAt)"
    }
}