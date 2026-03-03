from fastapi import Request
from loguru import logger
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.responses import Response


class GroupIdLoggingMiddleware(BaseHTTPMiddleware):
    """Gateway가 주입한 X-Group-Id(traceId)를 로그에 출력하는 미들웨어"""

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        group_id = request.headers.get("X-Group-Id", "-")
        with logger.contextualize(group_id=group_id):
            logger.info("{} {} | groupId={}", request.method, request.url.path, group_id)
            response = await call_next(request)
            return response
