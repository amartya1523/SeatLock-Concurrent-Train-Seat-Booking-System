.PHONY: up down logs test build

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f

test:
	cd backend && ./mvnw test
	cd frontend && npm run lint && npm run build

build:
	cd backend && ./mvnw clean package
	cd frontend && npm run build
