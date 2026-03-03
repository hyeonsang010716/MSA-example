from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.store.postgres.aio import AsyncPostgresStore
from langgraph.store.postgres.base import PoolConfig
from psycopg.rows import dict_row
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import uvicorn
import py_eureka_client.eureka_client as eureka_client


from app.config.setting import settings
from app.core.llm_manager import get_llm_manager
from app.core.graph.example.graph_orchestrator import get_example_graph
from app.core.graph.stream.stream_graph import get_stream_graph
from app.core.graph.handoffs.handoffs_graph import get_handoffs_graph
from app.api.v1.router import router as v1_router
from app.api.graphql.router import graphql_router
from app.api.health import router as health_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 생명주기 관리"""
    
    # LLM 초기화
    get_llm_manager()

    # Eureka 등록
    await eureka_client.init_async(
        eureka_server=settings.EUREKA_SERVER,
        app_name=settings.EUREKA_APP_NAME,
        instance_port=settings.PORT,
        instance_host=settings.EUREKA_INSTANCE_HOST,
        health_check_url=f"http://{settings.EUREKA_INSTANCE_HOST}:{settings.PORT}/actuator/health",
        home_page_url=f"http://{settings.EUREKA_INSTANCE_HOST}:{settings.PORT}/",
        status_page_url=f"http://{settings.EUREKA_INSTANCE_HOST}:{settings.PORT}/docs",
        renewal_interval_in_secs=30,
        duration_in_secs=90,
    )

    async with (
        AsyncPostgresStore.from_conn_string(
            conn_string=settings.POSTGRES_URL,
            pool_config=PoolConfig(
                min_size=5,
                max_size=20,
                kwargs={
                    "autocommit": True,
                    "prepare_threshold": 0,
                    "row_factory": dict_row,
                },
            )
        ) as store,
        AsyncPostgresSaver.from_conn_string(settings.POSTGRES_URL) as checkpointer,
    ):
        # 첫 실행 시 테이블 생성
        await store.setup()
        await checkpointer.setup()

        example_graph = get_example_graph()
        await example_graph.initialize(store, checkpointer)

        stream_graph = get_stream_graph()
        await stream_graph.initialize(store, checkpointer)

        handoffs_graph = get_handoffs_graph()
        await handoffs_graph.initialize(store, checkpointer)

        app.state.checkpointer = checkpointer

        yield

    # Eureka 해제
    await eureka_client.stop_async()


def create_app() -> FastAPI:
    """FastAPI 애플리케이션 팩토리"""
    
    app = FastAPI(
        title="FastAPI Server",
        description="FastAPI 서버 공통 세팅",
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )
    
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://127.0.0.1:5500", "http://127.0.0.1:5501"], # Frontend URL 권한 부여
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    app.include_router(health_router)
    app.include_router(v1_router)
    app.include_router(graphql_router, prefix="/graphql")

    return app


app = create_app()


# uv run uvicorn app.main:app --host 0.0.0.0 --port 8084
if __name__ == "__main__":
    uvicorn.run(
        app,
        host=settings.HOST,
        port=settings.PORT,
        reload=False,
        log_config=None,
    )