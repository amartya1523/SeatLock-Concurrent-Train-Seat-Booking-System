.PHONY: up down logs test build demo reset

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f

demo:
	node scripts/concurrency-demo.mjs

reset:
	docker compose down -v
	docker compose up --build -d

test:
	cd backend && ./mvnw test
	cd frontend && npm run lint && npm run build

build:
	cd backend && ./mvnw clean package
	cd frontend && npm run build
