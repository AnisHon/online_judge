ARG JUDGE_ENVIRONMENT_IMAGE=oj-judge-environment:2026.09
FROM ${JUDGE_ENVIRONMENT_IMAGE}

LABEL org.opencontainers.image.title="延拓Code GoJudge sandbox"
LABEL org.opencontainers.image.description="Small runtime layer over the reusable judge environment image"

COPY run.sh /opt/run.sh
RUN chmod 0755 /opt/run.sh

ENTRYPOINT ["/opt/run.sh"]
