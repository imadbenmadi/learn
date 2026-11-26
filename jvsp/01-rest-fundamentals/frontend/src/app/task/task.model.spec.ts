import { TaskStatus, allowedTransitions, label } from './task.model';

describe('allowedTransitions', () => {
  // Same table as TaskStatusTest on the backend: the UI must not offer what the server forbids.
  it.each<[TaskStatus, TaskStatus[]]>([
    ['TODO', ['IN_PROGRESS', 'CANCELLED']],
    ['IN_PROGRESS', ['TODO', 'DONE', 'CANCELLED']],
    ['DONE', []],
    ['CANCELLED', []],
  ])('from %s allows %j', (from, expected) => {
    expect(allowedTransitions(from)).toEqual(expected);
  });
});

describe('label', () => {
  it('turns enum constants into words', () => {
    expect(label('IN_PROGRESS')).toBe('In progress');
    expect(label('HIGH')).toBe('High');
  });
});
