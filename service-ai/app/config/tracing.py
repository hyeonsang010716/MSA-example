from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.zipkin.json import ZipkinExporter
from opentelemetry.sdk.resources import Resource, SERVICE_NAME
from opentelemetry.propagators.b3 import B3MultiFormat
from opentelemetry.propagate import set_global_textmap
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from fastapi import FastAPI


def setup_tracing(app: FastAPI, service_name: str, zipkin_endpoint: str) -> None:
    """OpenTelemetry 트레이싱 설정 (B3 전파 + Zipkin 리포트)"""

    resource = Resource.create({SERVICE_NAME: service_name})
    provider = TracerProvider(resource=resource)

    exporter = ZipkinExporter(endpoint=zipkin_endpoint)
    provider.add_span_processor(BatchSpanProcessor(exporter))

    trace.set_tracer_provider(provider)
    set_global_textmap(B3MultiFormat())

    FastAPIInstrumentor.instrument_app(app)
