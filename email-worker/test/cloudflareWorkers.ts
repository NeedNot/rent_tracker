export class WorkerEntrypoint<Env> {
  constructor(
    protected readonly ctx: ExecutionContext,
    protected readonly env: Env,
  ) {}
}
