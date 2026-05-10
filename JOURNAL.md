# Diário de Bordo — EnergyTrack API

> Ao final de cada sessão: o que fiz, o que aprendi, o que travou (e como resolvi), e o que farei na próxima sessão.

---

## 2026-05-10 — Sessão 1: Bootstrap da Fase 0

### O que fiz hoje

- **Toolchain**: instalei o SDKMAN! via curl (depois de instalar `bash` 5 com Homebrew, porque o instalador exige bash 4+) e o Temurin 21.0.11 (LTS). Pinei a versão por projeto via `.sdkmanrc` (`java=21.0.11-tem`) e habilitei `sdkman_auto_env=true` no `~/.sdkman/etc/config`.
- **Limpeza do `~/.zshrc`**: removi referências mortas (`zsh-syntax-highlighting` em caminhos inexistentes, autocomplete do `ng` quebrado), troquei `ZSH_THEME` para string vazia (o p10k já é carregado direto via brew), e reconfigurei o p10k via `p10k configure` (ASCII puro, sem ícones, *transient prompt* ativo).
- **Gradle**: refatorei `settings.gradle.kts` com `pluginManagement` + `dependencyResolutionManagement` e `FAIL_ON_PROJECT_REPOS`. Criei `gradle/libs.versions.toml` (version catalog) com versões de Spring Boot 3.5, Spring Modulith, Flyway, Postgres, Testcontainers, SpringDoc e OpenAPI Generator. Reescrevi `build.gradle.kts` (Kotlin DSL) usando o catálogo via `alias()` e bundles (`spring-app`, `testing-integration`).
- **OpenAPI spec-first**: criei `api-spec/openapi.yaml` (OpenAPI 3.1) com `GET /api/v1/health` e schema `HealthStatus` (enum `UP`/`DOWN`). Configurei as tasks `openApiValidate` e `openApiGenerate` (output em `build/generated/openapi`, `interfaceOnly = true`, `useTags = true`, pacotes `com.massari.energytracker.api.generated[.model]`). Fiz `compileJava` depender de `openApiGenerate` para regenerar antes de compilar.
- **Validações**: `./gradlew openApiValidate` → ok. `./gradlew openApiGenerate` → gerou `HealthApi.java` (interface anotada com `@RequestMapping` etc.) e `HealthStatus.java` (classe com `StatusEnum` inner). `./gradlew compileJava` → ok depois de adicionar a dep do SpringDoc.
- **Commit & push**: `2229e67` — *Bootstrap Phase 0: version catalog + OpenAPI spec-first pipeline*. 4 arquivos, +177 / -13. Enviado para `origin/main`.

### O que aprendi

- Toolchain do Gradle tem **duas JVMs distintas**: a *launcher* (que roda o Gradle) e a *target* (do `java { toolchain { ... } }`). Misturar é a causa de erros tipo `What went wrong: 25.0.2` quando o JDK do `PATH` é mais novo do que o Gradle suporta.
- `FAIL_ON_PROJECT_REPOS` é o tipo de política estrita que parece picuinha até ser usada — quebra cedo em violações de centralização de repositório.
- O objeto `libs` do version catalog é **gerado em sync time** do IntelliJ. Quando ele está vermelho, o problema é sync (não código).
- O OpenAPI Generator pressupõe `swagger-annotations` no classpath. Sem isso, o código gerado parece quebrado, mas é o projeto que falta dep. SpringDoc traz o pacote transitivamente **e** dá a UI de graça — vale a pena adotar logo.
- `openApiGenerate` e `openApiValidate` são tasks distintas com `inputSpec` independente. Configurar uma não propaga para a outra.
- `@Incubating` ≠ `@Deprecated`. A primeira é "use sabendo que ainda pode receber polimento"; a segunda é "vai sumir".

### O que travou e como resolvi

| Bloqueio | Causa | Resolução |
|---|---|---|
| `./gradlew build` falhava com erro `25.0.2` | JDK 25 no `PATH`; Gradle 8.14 suporta launcher até Java 24 | SDKMAN + Temurin 21 + `.sdkmanrc` |
| Instalador SDKMAN abortava: "Bash 4 required" | macOS tem bash 3.2 do sistema (GPLv3) | `brew install bash`, re-rodar via `/opt/homebrew/bin/bash` |
| Instalador SDKMAN falhava em `touch ~/.bash_profile` | Arquivo era `root:wheel` por culpa de algum instalador antigo | `sudo chown massari:staff ~/.bash_profile` |
| Após editar `build.gradle.kts`, IntelliJ desligou autocomplete | Typo `libs.plugins.dependency.management` (correto: `libs.plugins.spring.dependency.management`) → script não compila → `libs.*` some | Corrigir o alias e fazer Reload Gradle |
| `openApiValidate` reclamava de `inputSpec` não configurado | A task `openApiValidate` é independente de `openApiGenerate` | Extrair caminho da spec para `val openApiSpec` e configurar ambas |
| `compileJava` falhava com imports `io.swagger.v3.oas.annotations` ausentes | Generator usa anotações Swagger; sem `swagger-annotations` no classpath, não compila | Adicionar `springdoc-openapi-starter-webmvc-ui` ao catálogo e às deps |

### O que farei na próxima sessão

1. Criar `src/main/resources/application.yml` com profiles `dev`, `test`, `prod` e config de `DataSource`.
2. Criar `docker-compose.yml` na raiz com PostgreSQL 16 (volume persistido, porta 5432).
3. Criar `src/main/resources/db/migration/V001__init.sql` vazia (apenas para o Flyway baseline).
4. Validar `./gradlew bootRun` subindo a aplicação e conectando no Postgres local.
5. Implementar `com.massari.energytracker.health.HealthController` que estende `HealthApi`, retornando `ResponseEntity.ok(new HealthStatus().status(StatusEnum.UP))`.
6. Configurar SpringDoc para servir a spec **estática** (não code-first): copiar `api-spec/openapi.yaml` para `src/main/resources/static/openapi.yaml` no build (ou usar `springdoc.swagger-ui.url`) e ajustar `application.yml`.
7. Validação visual no browser (`/swagger-ui.html`) e via `curl http://localhost:8080/api/v1/health`.

### Decisões deliberadas a lembrar

- **Spring Security adiada para Fase 3** — declarar a dep agora bloquearia `/health` e `/swagger-ui` por default.
- **DTOs gerados como `class` (não `record`)** — é o template default do generator `spring`. Aceitável agora; refatorar para records é exercício deliberado mais tarde.
- **Catálogo monolítico em `api-spec/openapi.yaml`** — o split em `paths/`, `components/schemas/` (descrito no PLAN seção 4) só passa a valer quando tivermos 5-6 endpoints; até lá, monolito é mais simples.
