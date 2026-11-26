// The parts of the Actuator JSON this UI reads. Every endpoint wraps its data per application
// context ("contexts": {"task-api": {...}}); a plain Boot app has exactly one.

export interface Contexts<T> {
  readonly contexts: Readonly<Record<string, T>>;
}

export interface BeanDescriptor {
  readonly type: string;
  readonly scope: string;
  readonly resource: string | null;
  readonly dependencies: readonly string[];
}

export interface BeansContext {
  readonly beans: Readonly<Record<string, BeanDescriptor>>;
}

export interface ConditionOutcome {
  readonly condition: string;
  readonly message: string;
}

export interface ConditionsContext {
  readonly positiveMatches: Readonly<Record<string, readonly ConditionOutcome[]>>;
  readonly negativeMatches: Readonly<
    Record<
      string,
      {
        readonly notMatched: readonly ConditionOutcome[];
        readonly matched: readonly ConditionOutcome[];
      }
    >
  >;
  readonly unconditionalClasses: readonly string[];
}

export interface HandlerMapping {
  readonly handler: string;
  readonly predicate: string;
  readonly details: {
    readonly handlerMethod?: { readonly className: string; readonly name: string };
  } | null;
}

export interface MappingsContext {
  readonly mappings: {
    readonly dispatcherServlets?: Readonly<Record<string, readonly HandlerMapping[]>>;
  };
}

export interface ConfigPropsContext {
  readonly beans: Readonly<
    Record<string, { readonly prefix: string; readonly properties: unknown }>
  >;
}

/** Actuator always reports one context for this app; take the first. */
export function firstContext<T>(value: Contexts<T> | undefined): T | null {
  if (!value) return null;
  const all = Object.values(value.contexts);
  return all.length ? all[0] : null;
}
