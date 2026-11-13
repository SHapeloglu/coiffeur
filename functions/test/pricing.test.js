"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { expectedTotalTL, isPriceValid, averageRating } = require("../lib/pricing");

test("expectedTotalTL: indirim yok, tam fiyat", () => {
  assert.equal(expectedTotalTL(100, 0), 100);
});

test("expectedTotalTL: %100 indirim, ücretsiz", () => {
  assert.equal(expectedTotalTL(100, 100), 0);
});

test("expectedTotalTL: normal indirim", () => {
  assert.equal(expectedTotalTL(100, 15), 85);
});

test("expectedTotalTL: 2 ondalık yuvarlama (33.33 * 0.9 = 29.997 -> 30.00)", () => {
  assert.equal(expectedTotalTL(33.33, 10), 30);
});

test("expectedTotalTL: 2 ondalık yuvarlama (10 * 0.67 = 6.7)", () => {
  assert.equal(expectedTotalTL(10, 33), 6.7);
});

test("expectedTotalTL: indirim 0-100 aralığına sıkıştırılır", () => {
  assert.equal(expectedTotalTL(100, 150), 0);
  assert.equal(expectedTotalTL(100, -5), 100);
});

test("isPriceValid: tam eşleşme", () => {
  assert.equal(isPriceValid(85, 100, 15), true);
});

test("isPriceValid: tolerans sınırı içinde (0.01) kabul", () => {
  assert.equal(isPriceValid(85.01, 100, 15), true);
  assert.equal(isPriceValid(84.99, 100, 15), true);
});

test("isPriceValid: tolerans dışında (0.02) red", () => {
  assert.equal(isPriceValid(85.02, 100, 15), false);
  assert.equal(isPriceValid(84.98, 100, 15), false);
});

test("isPriceValid: %100 indirimde 0 TL kabul, 0.02 red", () => {
  assert.equal(isPriceValid(0, 100, 100), true);
  assert.equal(isPriceValid(0.02, 100, 100), false);
});

test("isPriceValid: sayı olmayan veya NaN değerler red", () => {
  assert.equal(isPriceValid("85", 100, 15), false);
  assert.equal(isPriceValid(NaN, 100, 15), false);
  assert.equal(isPriceValid(undefined, 100, 15), false);
  assert.equal(isPriceValid(85, NaN, 15), false);
});

test("averageRating: boş liste 0", () => {
  assert.equal(averageRating([]), 0);
});

test("averageRating: tek puan", () => {
  assert.equal(averageRating([5]), 5);
});

test("averageRating: 2 ondalığa yuvarlama (14/3 -> 4.67)", () => {
  assert.equal(averageRating([4, 5, 5]), 4.67);
});

test("averageRating: tam ortalama", () => {
  assert.equal(averageRating([1, 2]), 1.5);
});

test("averageRating: 4/3 -> 1.33", () => {
  assert.equal(averageRating([1, 1, 2]), 1.33);
});
