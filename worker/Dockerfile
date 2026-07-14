# Stage 1: Build TypeScript Application
FROM node:20-alpine AS builder
WORKDIR /app

# Copy configuration and package files
COPY package.json package-lock.json* ./
RUN npm install

# Copy source code
COPY tsconfig.json ./
COPY src ./src

# Build TypeScript to JavaScript
RUN npm run build

# Stage 2: Runtime Image
FROM node:20-alpine
WORKDIR /app

# Copy production files
COPY --from=builder /app/dist ./dist
COPY package.json package-lock.json* ./

# Install only production dependencies
RUN npm install --only=production

ENTRYPOINT ["node", "dist/main.js"]
