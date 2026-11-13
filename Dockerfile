FROM node:22-alpine AS frontend-build
WORKDIR /workspace
RUN npm install --global pnpm@11.25.0
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY frontend/package.json frontend/package.json
RUN pnpm install --frozen-lockfile
COPY frontend ./frontend
RUN pnpm --dir frontend build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
ENV PORT=3016
EXPOSE 3016
COPY --from=backend-build /workspace/backend/target/*.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--XX:MaxRAMPercentage=75.0} -jar /app/app.jar --server.port=${PORT:-3016}"]
