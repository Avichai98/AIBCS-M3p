from fastapi import Request, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError
from config.env_loader import get_jwt_secret

class JWTBearer(HTTPBearer):
    def __init__(self, auto_error: bool = True):
        super(JWTBearer, self).__init__(auto_error=auto_error)

    async def __call__(self, request: Request):
        credentials: HTTPAuthorizationCredentials = await super().__call__(request)
        if credentials:
            try:
                payload = jwt.decode(
                    credentials.credentials,
                    get_jwt_secret(),
                    algorithms=["HS256"]
                )
                return payload
            except JWTError:
                raise HTTPException(status_code=403, detail="Invalid or expired token")
        raise HTTPException(status_code=403, detail="Invalid authorization token")
