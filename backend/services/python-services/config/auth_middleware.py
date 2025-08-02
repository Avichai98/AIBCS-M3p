import os
from fastapi import Request, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError
from typing import List, Union # חשוב לייבא את זה!

class JWTBearer(HTTPBearer):
    """
    A FastAPI dependency that handles JWT token authentication.

    It extracts the token from the Authorization header, decodes it,
    and returns the payload if valid. Raises HTTPException on failure.
    """
    def __init__(self, auto_error: bool = True):
        """
        Initializes the JWTBearer.

        :param auto_error: If True, raises HTTPException on authentication failure.
                           If False, returns None for credentials on failure.
        """
        super().__init__(auto_error=auto_error)

    async def __call__(self, request: Request) -> dict:
        """
        Authenticates the incoming request by validating the JWT token.

        :param request: The incoming FastAPI Request object.
        :return: The decoded JWT payload as a dictionary if authentication is successful.
        :raises HTTPException: If the token is missing, invalid, expired, or the secret is not configured.
        """
        # Attempt to get credentials from the Authorization header
        credentials: HTTPAuthorizationCredentials = await super().__call__(request)

        if credentials:
            # Ensure the JWT_SECRET environment variable is set
            jwt_secret = os.getenv("JWT_SECRET")
            if not jwt_secret:
                # Log this error as it indicates a configuration issue
                print("Error: JWT_SECRET environment variable is not set.")
                raise HTTPException(
                    status_code=500,
                    detail="Server configuration error: JWT secret not found."
                )

            try:
                # Decode the JWT token
                payload = jwt.decode(
                    credentials.credentials,  # The actual token string
                    jwt_secret,               # The secret key for decoding
                    algorithms=["HS384"]      # The algorithm used for signing
                )
                return payload
            except JWTError as e:
                # Log the specific JWT error for debugging
                print(f"JWT Authentication Error: {e}")
                raise HTTPException(status_code=403, detail="Invalid or expired token")
            except Exception as e:
                # Catch any other unexpected errors during processing
                print(f"An unexpected error occurred during token processing: {e}")
                raise HTTPException(status_code=500, detail="Authentication failed due to server error.")

        # If no credentials were provided in the request
        raise HTTPException(status_code=403, detail="Authorization token is missing or invalid.")


# --- This is the roles_required function you need to add! ---

def roles_required(required_roles: Union[str, List[str]]):
    """
    FastAPI dependency to enforce role-based access control.

    :param required_roles: A single role string (e.g., "admin") or a list of role strings (e.g., ["admin", "user"]).
                           The user must have at least one of the required roles.
    :return: A dependency function that returns the JWT payload if roles are sufficient,
             otherwise raises an HTTPException.
    """
    # Ensure required_roles is always a list for consistent processing
    if isinstance(required_roles, str):
        required_roles = [required_roles]

    def role_checker(payload: dict = Depends(JWTBearer())):
        """
        Inner dependency function that performs the role check.
        It uses JWTBearer to get the authenticated payload.
        """
        user_roles: List[str] = payload.get("roles", []) # Get user roles from JWT payload, default to empty list

        # Check if any of the user's roles are in the list of required roles
        # If no common roles are found, access is denied.
        if not any(role in user_roles for role in required_roles):
            raise HTTPException(
                status_code=403,
                detail=f"Insufficient permissions. Requires one of the following roles: {', '.join(required_roles)}"
            )
        return payload # Return the payload if roles are sufficient
    return role_checker