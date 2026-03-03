from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/actuator/health")
async def health_check():
    """Eureka 헬스체크 엔드포인트"""
    return {"status": "UP"}
