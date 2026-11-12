export function* takeWhile<T>(items: Iterable<T>, keep: (item: T) => boolean): Generator<T> {
  for (const item of items) {
    if (!keep(item)) {
      return;
    }
    yield item;
  }
}

export function* dropWhile<T>(items: Iterable<T>, drop: (item: T) => boolean): Generator<T> {
  let dropping = true;
  for (const item of items) {
    dropping = dropping && drop(item);
    if (!dropping) {
      yield item;
    }
  }
}

export function* take<T>(items: Iterable<T>, count: number): Generator<T> {
  if (count <= 0) {
    return;
  }
  let taken = 0;
  for (const item of items) {
    yield item;
    taken += 1;
    if (taken === count) {
      return;
    }
  }
}

export function first<T>(items: Iterable<T>): T | undefined {
  for (const item of items) {
    return item;
  }
  return undefined;
}
