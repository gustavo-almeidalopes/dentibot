import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import test from 'node:test';
import { sha256Hex } from './anexos.js';

test('o SHA-256 do navegador é o mesmo que o back-end e o sha256sum calculam', async () => {
  const bytes = new TextEncoder().encode('radiografia panoramica');
  assert.equal(await sha256Hex(bytes), createHash('sha256').update(bytes).digest('hex'));
});
