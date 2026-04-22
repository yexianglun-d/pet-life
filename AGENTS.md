# PetLife Project Agent Rules

## Project Identity

- Project code name: `petlife`
- Product name: `宠物生活管家`
- Current delivery scope: `mobile-app + server + admin-web`
- Deferred scope: `commerce backend`, `device vendor integration`

## Naming Conventions

- Java root package: `com.petlife.server`
- Java module package: `com.petlife.server.modules.<domain>`
- Maven coordinates:
  - `groupId`: `com.petlife`
  - `artifactId`: `petlife-server`
- Admin web npm package: `@petlife/admin-web`
- Flutter package name: `petlife_mobile_app`
- Mobile application id: `com.petlife.mobile`

## Engineering Rules

1. Follow structured, enterprise-grade naming. Avoid ambiguous names such as `util`, `manager`, `handler2`, `temp`, `test1`.
2. Follow Alibaba Java Manual for business code, package layout and comments.
3. Add concise Javadoc or block comments for core business rules, critical state transitions and non-obvious code paths.
4. Do not use patch-style bug fixes. Fix root cause and keep module boundaries clear.
5. Prefer vertical business modules on the server side. Cross-module concerns go into `common` or `config`.
6. Keep deferred capabilities behind explicit placeholders or reserved interfaces. Do not expose half-finished backend chains.
7. All UI implementation in `mobile-app` and `admin-web` must follow `DESIGN.md`.

## Server Code Structure

Implemented server modules must follow this package layout:

- `controller`: interface layer, only receive request DTO and return response DTO.
- `dto.request`: request models.
- `dto.response`: response models.
- `service`: application service orchestration layer.
- `domain.entity`: stable business entity models.
- `converter`: conversion between `DataObject`, `Entity` and response DTO.
- `persistence`: MyBatis Mapper interfaces.
- `persistence.dataobject`: persistence read models.
- `persistence.command`: persistence write command models.

## Java Type Naming Rules

- Controller classes: `*Controller`
- Application services: `*ApplicationService`
- Domain entities: `*Entity`
- Persistence read models: `*DataObject`
- Persistence write commands: `*Command`
- MyBatis mappers: `*PersistenceMapper`
- Request DTO: `*Request`
- Response DTO: `*Response`
- Cross-module view converters: `*Converter`

## Layer Boundary Rules

1. `controller` must not directly depend on `persistence`.
2. `service` must not expose `DataObject` to controllers or clients.
3. `persistence` only returns `DataObject` or accepts `Command`; it must not return response DTO.
4. `converter` is the only layer allowed to translate between `DataObject` and `Entity`, or `Entity` and response DTO.
5. Cross-module response reuse must be handled by converters, not by one application service calling another service's private view assembly logic.
6. Core business rules, default initialization, state normalization and non-obvious mapping logic must include concise Javadoc or block comments.

## Design Baseline

- UI design guideline file: `DESIGN.md`
- Default visual direction:
  - restrained
  - clear
  - trustworthy
  - modern
  - orderly
- Avoid:
  - flashy gradients
  - decorative overload
  - multi-focus first screens
  - obvious AI-generated visual patterns

## Current Module Baseline

- Implement now:
  - `auth`
  - `user`
  - `family`
  - `pet`
  - `health`
  - `reminder`
  - `dailylog`
  - `timeline`
  - `community`
  - `service`
  - `admin`
  - `notification`
  - `moderation`
- Reserve only:
  - `commerce`
  - `device`

## Collaboration Rule

When expanding the project, update this file if the package naming, project scope or coding baseline changes.
