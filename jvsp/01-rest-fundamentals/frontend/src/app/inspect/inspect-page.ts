import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import {
  BeansContext,
  ConditionsContext,
  ConfigPropsContext,
  Contexts,
  MappingsContext,
  firstContext,
} from './actuator';

type Tab = 'beans' | 'conditions' | 'mappings' | 'configprops';

const OWN_PACKAGE = 'dev.jvsp.';

/**
 * A view into the running ApplicationContext through Actuator. These endpoints are only exposed by
 * application-dev.yaml, so this page needs the backend started with the dev profile.
 */
@Component({
  selector: 'app-inspect-page',
  imports: [JsonPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './inspect-page.html',
})
export class InspectPage {
  protected readonly tab = signal<Tab>('beans');
  protected readonly filter = signal('');
  protected readonly ownOnly = signal(true);

  // Each resource only fetches once its tab is opened (undefined request = idle).
  private readonly beansRes = httpResource<Contexts<BeansContext>>(() =>
    this.tab() === 'beans' ? '/actuator/beans' : undefined,
  );
  private readonly conditionsRes = httpResource<Contexts<ConditionsContext>>(() =>
    this.tab() === 'conditions' ? '/actuator/conditions' : undefined,
  );
  private readonly mappingsRes = httpResource<Contexts<MappingsContext>>(() =>
    this.tab() === 'mappings' ? '/actuator/mappings' : undefined,
  );
  private readonly configPropsRes = httpResource<Contexts<ConfigPropsContext>>(() =>
    this.tab() === 'configprops' ? '/actuator/configprops' : undefined,
  );

  protected readonly current = computed(() => {
    switch (this.tab()) {
      case 'beans':
        return this.beansRes;
      case 'conditions':
        return this.conditionsRes;
      case 'mappings':
        return this.mappingsRes;
      case 'configprops':
        return this.configPropsRes;
    }
  });

  protected readonly unavailable = computed(() => {
    const error = this.current().error();
    return error !== undefined;
  });

  protected readonly beans = computed(() => {
    const ctx = firstContext(this.beansRes.value());
    if (!ctx) return null;
    const all = Object.entries(ctx.beans).map(([name, bean]) => ({ name, ...bean }));
    const shown = all
      .filter((b) => !this.ownOnly() || b.type.startsWith(OWN_PACKAGE))
      .filter((b) => this.matches(b.name, b.type))
      .sort((a, b) => a.name.localeCompare(b.name));
    return { total: all.length, shown };
  });

  protected readonly conditions = computed(() => {
    const ctx = firstContext(this.conditionsRes.value());
    if (!ctx) return null;
    const positive = Object.entries(ctx.positiveMatches)
      .filter(([cls]) => this.matches(cls))
      .map(([cls, outcomes]) => ({ cls, outcomes }));
    const negative = Object.entries(ctx.negativeMatches)
      .filter(([cls]) => this.matches(cls))
      .map(([cls, result]) => ({ cls, outcomes: result.notMatched }));
    return {
      positiveTotal: Object.keys(ctx.positiveMatches).length,
      negativeTotal: Object.keys(ctx.negativeMatches).length,
      positive,
      negative,
    };
  });

  protected readonly mappings = computed(() => {
    const ctx = firstContext(this.mappingsRes.value());
    if (!ctx) return null;
    return Object.values(ctx.mappings.dispatcherServlets ?? {})
      .flat()
      .filter(
        (m) =>
          !this.ownOnly() || (m.details?.handlerMethod?.className ?? '').startsWith(OWN_PACKAGE),
      )
      .filter((m) => this.matches(m.predicate, m.handler));
  });

  protected readonly configProps = computed(() => {
    const ctx = firstContext(this.configPropsRes.value());
    if (!ctx) return null;
    return Object.entries(ctx.beans)
      .map(([bean, value]) => ({ bean, ...value }))
      .filter((p) => !this.ownOnly() || p.bean.includes(OWN_PACKAGE))
      .filter((p) => this.matches(p.prefix, p.bean))
      .sort((a, b) => a.prefix.localeCompare(b.prefix));
  });

  protected onFilter(event: Event): void {
    this.filter.set((event.target as HTMLInputElement).value);
  }

  protected shortName(className: string): string {
    return className.substring(className.lastIndexOf('.') + 1);
  }

  private matches(...texts: string[]): boolean {
    const needle = this.filter().trim().toLowerCase();
    return needle === '' || texts.some((t) => t.toLowerCase().includes(needle));
  }
}
