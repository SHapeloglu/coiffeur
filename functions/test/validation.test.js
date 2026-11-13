"use strict";

const { test } = require("node:test");
const assert = require("node:assert/strict");
const { isSafeId, slotStartMs, isPastSlot } = require("../lib/validation");

test("isSafeId: boş olmayan, '/' içermeyen string kabul edilir", () => {
  assert.equal(isSafeId("slot1"), true);
  assert.equal(isSafeId("2026-12-01_1000"), true);
});

test("isSafeId: '/' içeren, boş, string olmayan değerler reddedilir (V8)", () => {
  assert.equal(isSafeId("x/y"), false);
  assert.equal(isSafeId("/"), false);
  assert.equal(isSafeId(""), false);
  assert.equal(isSafeId(undefined), false);
  assert.equal(isSafeId(42), false);
});

test("slotStartMs: Türkiye saati (+03:00) ile epoch ms üretir", () => {
  // 2026-12-01 10:00 TRT = 07:00 UTC
  assert.equal(slotStartMs("2026-12-01", "10:00"), Date.UTC(2026, 11, 1, 7, 0));
});

test("slotStartMs: biçimi bozuk tarih/saat NaN döner", () => {
  assert.ok(Number.isNaN(slotStartMs("01.12.2026", "10:00")));
  assert.ok(Number.isNaN(slotStartMs("2026-12-01", "10")));
  assert.ok(Number.isNaN(slotStartMs(undefined, "10:00")));
});

test("isPastSlot: başlangıç geçmişteyse true (V9)", () => {
  const now = Date.UTC(2026, 10, 13, 12, 0); // 2026-11-13 15:00 TRT
  assert.equal(isPastSlot("2020-01-01", "10:00", now), true);
  assert.equal(isPastSlot("2026-11-13", "10:00", now), true); // bugünkü geçmiş saat
});

test("isPastSlot: başlangıç gelecekteyse false", () => {
  const now = Date.UTC(2026, 10, 13, 12, 0);
  assert.equal(isPastSlot("2026-11-13", "18:00", now), false);
  assert.equal(isPastSlot("2026-12-01", "10:00", now), false);
});

test("isPastSlot: tam başlangıç anı geçmiş sayılır (<=)", () => {
  const now = Date.UTC(2026, 11, 1, 7, 0); // 2026-12-01 10:00 TRT
  assert.equal(isPastSlot("2026-12-01", "10:00", now), true);
});

test("isPastSlot: okunamayan tarih geçmiş sayılmaz (INVALID_SLOT_TIME ayrıca raporlanır)", () => {
  assert.equal(isPastSlot("bozuk", "10:00", Date.now()), false);
});
