// Сборка презентации по лабораторной работе № 4 (pptxgenjs)
const path = require('path');
const fs = require('fs');
const pptxgen = require('pptxgenjs');

const PROJECT = '/Users/azatminyazov/Workspace/university java/4 work';
const OUT = process.env.OUT || path.join(PROJECT, 'docs/presentation/Презентация_ЛР4_Минязов.pptx');
const shot = n => path.join(PROJECT, 'docs/screenshots', n);
const uml = n => path.join(PROJECT, 'docs/uml', n);

// Палитра окна программы: графит, стрелки, +1, −1, робот
const C = {
  dark: '2C2C2A', ink: '2C2C2A', muted: '6B6A65', light: 'F7F6F3', line: 'E4E2DC',
  path: '1D9E75', pathBg: 'E1F5EE', water: '185FA5', waterBg: 'E6F1FB', okText: '085041',
  shock: 'A32D2D', shockBg: 'FCEBEB', cheese: 'BA7517', cheeseBg: 'FAEEDA', white: 'FFFFFF',
};
const HEAD = 'Cambria';
const BODY = 'Calibri';

function pngSize(file) {
  const b = fs.readFileSync(file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}

/** Картинка, вписанная в прямоугольник с сохранением пропорций (по центру). */
function fit(slide, file, x, y, w, h, extra = {}) {
  const s = pngSize(file);
  const k = Math.min(w / s.w, h / s.h);
  const iw = s.w * k, ih = s.h * k;
  slide.addImage({ path: file, x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih, ...extra });
  return { x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih };
}

function title(slide, text, sub) {
  slide.addText(text, { x: 0.6, y: 0.4, w: 12.1, h: 0.8, fontFace: HEAD, fontSize: 36, bold: true, color: C.ink, margin: 0, isTextBox: true });
  if (sub) {
    slide.addText(sub, { x: 0.6, y: 1.15, w: 12.1, h: 0.45, fontFace: BODY, fontSize: 16, color: C.muted, margin: 0, isTextBox: true });
  }
}

function card(slide, x, y, w, h, fill) {
  slide.addShape('roundRect', { x, y, w, h, fill: { color: fill }, line: { color: fill }, rectRadius: 0.12 });
}

/** Кружок с цифрой или знаком — общий мотив всех слайдов. */
function badge(slide, x, y, d, fill, text, color = C.white, size = 16) {
  slide.addShape('ellipse', { x, y, w: d, h: d, fill: { color: fill }, line: { color: fill } });
  slide.addText(text, { x, y, w: d, h: d, align: 'center', valign: 'middle', fontFace: BODY, fontSize: size, bold: true, color, margin: 0, isTextBox: true });
}

// цвета ролей, как в окне
const ARROW = C.water, PLUS = C.path, MINUS = C.shock, ROBOT = 'D85A30';

const pres = new pptxgen();
pres.layout = 'LAYOUT_WIDE';  // 13.33 x 7.5
pres.author = 'А. А. Минязов';
pres.title = 'Стохастическое окружение — движение агента в стохастической среде';

const bullets = (slide, items, box, size = 17) => slide.addText(
  items.map((t, i) => ({ text: t, options: { bullet: true, breakLine: i < items.length - 1 } })),
  { ...box, fontFace: BODY, fontSize: size, color: C.ink, paraSpaceAfter: 10, margin: 0, valign: 'top', isTextBox: true });

// ---------- 1. Титул ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Лабораторная работа № 4 · ООП на языке Java', { x: 0.6, y: 0.7, w: 6.4, h: 0.4, fontFace: BODY, fontSize: 16, color: 'B4B2A9', margin: 0, isTextBox: true });
  s.addText('Стохастическое окружение', { x: 0.6, y: 1.5, w: 6.4, h: 1.4, fontFace: HEAD, fontSize: 40, bold: true, color: C.white, margin: 0, isTextBox: true });
  s.addText('Робота сносит ветром: программа находит стрелки, по которым он чаще всего доходит до +1', { x: 0.6, y: 3.0, w: 6.2, h: 1.3, fontFace: BODY, fontSize: 20, color: 'D3D1C7', margin: 0, isTextBox: true });
  s.addText([
    { text: 'Выполнил: ', options: { color: 'B4B2A9' } }, { text: 'А. А. Минязов, гр. 5130902/40202', options: { color: C.white, breakLine: true } },
    { text: 'Проверил: ', options: { color: 'B4B2A9' } }, { text: 'ст. преп. Д. С. Хасанов', options: { color: C.white, breakLine: true } },
    { text: 'СПбПУ, ИКНК · 2026', options: { color: 'B4B2A9' } },
  ], { x: 0.6, y: 5.4, w: 6.2, h: 1.3, fontFace: BODY, fontSize: 16, margin: 0, paraSpaceAfter: 4, isTextBox: true });
  fit(s, shot('gui_3_solved.png'), 7.2, 0.9, 5.6, 5.7);
  s.addNotes('Тема: марковский процесс принятия решений. Действия ненадёжны, агент ищет стратегию с наибольшей ожидаемой полезностью. Java, окно на JavaFX.');
}

// ---------- 2. Задача ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Задача', 'Поле 4×3: старт (1,1), выход +1 в (4,3), выход −1 в (4,2), стена (2,2)');
  bullets(s, [
    'Среда полностью наблюдаема: агент знает, где он.',
    'Ходы ненадёжны: куда хотел — 0,8, вбок влево и вправо — по 0,1, назад — никогда.',
    'В стену или за край — остаётся на месте.',
    'Каждый шаг стоит R(s) = −0,04; выход даёт +1 или −1.',
    'Найти стратегию π(s) — стрелку в каждой клетке — с наибольшей ожидаемой суммой наград.',
  ], { x: 0.6, y: 1.9, w: 6.2, h: 4.8 });
  // схема сноса, как рисунок 1 задания
  const cx = 9.7, cy = 4.4;
  s.addShape('rect', { x: cx - 0.6, y: cy - 0.45, w: 1.2, h: 0.9, fill: { color: C.waterBg }, line: { color: ARROW, width: 1.5 } });
  s.addText('агент', { x: cx - 0.6, y: cy - 0.45, w: 1.2, h: 0.9, align: 'center', valign: 'middle', fontFace: BODY, fontSize: 14, color: ARROW, margin: 0, isTextBox: true });
  s.addShape('line', { x: cx, y: cy - 2.2, w: 0, h: 1.75, line: { color: ARROW, width: 4, beginArrowType: 'triangle' } });
  s.addShape('line', { x: cx - 2.4, y: cy, w: 1.8, h: 0, line: { color: C.muted, width: 3, beginArrowType: 'triangle' } });
  s.addShape('line', { x: cx + 0.6, y: cy, w: 1.8, h: 0, line: { color: C.muted, width: 3, endArrowType: 'triangle' } });
  s.addText('0,8 — куда хотел', { x: cx + 0.15, y: cy - 2.3, w: 2.8, h: 0.5, fontFace: BODY, fontSize: 18, bold: true, color: ARROW, margin: 0, isTextBox: true });
  s.addText('0,1', { x: cx - 2.4, y: cy - 0.6, w: 1.8, h: 0.45, align: 'center', fontFace: BODY, fontSize: 18, color: C.muted, margin: 0, isTextBox: true });
  s.addText('0,1', { x: cx + 0.6, y: cy - 0.6, w: 1.8, h: 0.45, align: 'center', fontFace: BODY, fontSize: 18, color: C.muted, margin: 0, isTextBox: true });
  s.addText('хочет вверх — ветер может снести вбок', { x: 7.3, y: 5.3, w: 4.8, h: 0.5, align: 'center', fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('Условие из задания. Назад ветер не сносит. Поэтому, например, при R = 0 робот упирается в стену спиной к −1 и ждёт безопасного сноса.');
}

// ---------- 3. Уравнение Беллмана ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Полезность клетки', 'U(s) — сколько робот в среднем наберёт, если стартует отсюда и ходит наилучшим образом');
  card(s, 0.6, 1.9, 12.1, 1.2, C.pathBg);
  s.addText('U(s) = R(s) + γ · max по ходам Σ T(s, a, s′) · U(s′)', { x: 0.9, y: 2.0, w: 11.5, h: 1.0, fontFace: 'Cambria Math', fontSize: 28, color: C.okText, margin: 0, valign: 'middle', isTextBox: true });
  const rows = [
    ['0,8 × 1', 'дошёл до +1', PLUS],
    ['0,1 × 0,918', 'снесло вверх в стену — остался', C.muted],
    ['0,1 × 0,660', 'снесло вниз в (3,2)', C.muted],
  ];
  s.addText('Клетка (3,3), ход направо:', { x: 0.6, y: 3.4, w: 6, h: 0.5, fontFace: BODY, fontSize: 18, bold: true, color: C.ink, margin: 0, isTextBox: true });
  rows.forEach(([v, d, col], i) => {
    const y = 4.0 + i * 0.7;
    s.addText(v, { x: 0.9, y, w: 2.4, h: 0.55, fontFace: HEAD, fontSize: 22, bold: true, color: col, margin: 0, valign: 'middle', isTextBox: true });
    s.addText(d, { x: 3.4, y, w: 4.0, h: 0.55, fontFace: BODY, fontSize: 16, color: C.ink, margin: 0, valign: 'middle', isTextBox: true });
  });
  s.addText('= 0,958 − 0,04 = 0,918', { x: 0.9, y: 6.2, w: 6.5, h: 0.6, fontFace: HEAD, fontSize: 26, bold: true, color: ARROW, margin: 0, isTextBox: true });
  card(s, 8.0, 3.4, 4.7, 3.4, C.light);
  bullets(s, [
    'R(s) — плата за шаг (−0,04) или награда выхода',
    'γ — во сколько раз дешевеет награда за шаг ожидания; в задании 1',
    'стрелка — ход, на котором достигается max',
  ], { x: 8.3, y: 3.6, w: 4.2, h: 3.0 }, 15);
  s.addNotes('Числа соседей тоже неизвестны — система связана, поэтому оба алгоритма считают приближениями. Ход вверх из (3,3) дал бы 0,881 — меньше, значит стрелка направо.');
}

// ---------- 4. Итерация по значениям ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Итерация по значениям', 'Сначала везде 0; каждую итерацию все клетки пересчитываются по числам соседей');
  fit(s, shot('gui_2_iter3.png'), 0.6, 1.8, 7.2, 5.3);
  const rows = [['1', '0,760'], ['3', '0,472'], ['10', '0,044'], ['20', '0,00035'], ['28', '< 0,000001']];
  s.addText('Итерация · наибольшее изменение U', { x: 8.1, y: 1.85, w: 4.6, h: 0.4, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
  rows.forEach(([n, d], i) => {
    const y = 2.35 + i * 0.75;
    card(s, 8.1, y, 4.6, 0.62, i === rows.length - 1 ? C.pathBg : C.light);
    s.addText(n, { x: 8.3, y, w: 1.0, h: 0.62, fontFace: HEAD, fontSize: 20, bold: true, color: ARROW, valign: 'middle', margin: 0, isTextBox: true });
    s.addText(d, { x: 9.4, y, w: 3.2, h: 0.62, fontFace: BODY, fontSize: 17, color: C.ink, valign: 'middle', margin: 0, isTextBox: true });
  });
  s.addText('Хорошие значения расходятся волной от +1. 28 итераций — и числа как в задании.', { x: 8.1, y: 6.2, w: 4.6, h: 0.9, fontFace: BODY, fontSize: 14, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('На первой итерации меняется только (3,3): −0,04 + 0,8 · 1 = 0,76. На второй — (2,3), потому что её сосед уже хороший. Конец, когда изменение меньше миллионной.');
}

// ---------- 5. Итерация по стратегиям ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Итерация по стратегиям', 'Стрелки наугад (все вверх), затем по очереди: оценка и улучшение');
  fit(s, shot('gui_4_pi_eval.png'), 0.6, 1.8, 5.9, 4.3);
  fit(s, shot('gui_5_pi_improve.png'), 6.8, 1.8, 5.9, 4.3);
  s.addText('Оценка: сколько дают текущие стрелки — числа меняются, стрелки нет', { x: 0.6, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: C.ink, margin: 0, isTextBox: true });
  s.addText('Улучшение: в каждой клетке лучший ход по этим числам — развернулось 6 стрелок', { x: 6.8, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: ARROW, bold: true, margin: 0, isTextBox: true });
  s.addNotes('Конец, когда при улучшении не поменялась ни одна стрелка: 5 итераций против 28, но каждая оценка — десятки и сотни проходов (первая — 854). Ответ тот же.');
}

// ---------- 6. Результат ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Результат', 'Оба алгоритма дают таблицу и стрелки задания');
  fit(s, shot('gui_3_solved.png'), 0.6, 1.8, 7.2, 5.3);
  const stats = [
    ['28', 'итераций по значениям', ARROW],
    ['5', 'итераций по стратегиям', ARROW],
    ['0,705', 'U старта = средняя сумма робота (0,706 по 2000 путям)', PLUS],
  ];
  stats.forEach(([v, l, col], i) => {
    s.addText(v, { x: 8.3, y: 1.9 + i * 1.6, w: 4.4, h: 0.8, fontFace: HEAD, fontSize: 38, bold: true, color: col, margin: 0, isTextBox: true });
    s.addText(l, { x: 8.3, y: 2.7 + i * 1.6, w: 4.4, h: 0.7, fontFace: BODY, fontSize: 15, color: C.muted, margin: 0, isTextBox: true });
  });
  s.addNotes('Из (3,1) налево, в обход: путь вверх идёт мимо −1, а там может снести. Налево 0,651 против 0,632 вверх.');
}

// ---------- 7. R(s) ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Как плата за шаг меняет стратегию', 'R(s) = −2 и R(s) = 0 на том же поле');
  fit(s, shot('gui_8_r_minus2.png'), 0.6, 1.8, 5.9, 4.3);
  fit(s, shot('gui_9_r_zero.png'), 6.8, 1.8, 5.9, 4.3);
  s.addText('R = −2: шаг дороже приза — из (3,2) и (4,1) прямо в −1, лишь бы закончить', { x: 0.6, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: MINUS, margin: 0, isTextBox: true });
  s.addText('R = 0: спешить некуда — упирается в стену спиной к −1 и ждёт безопасного сноса', { x: 6.8, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: PLUS, bold: true, margin: 0, isTextBox: true });
  s.addNotes('При R > 0 гулять выгоднее, чем выходить: при γ = 1 сумма бесконечна и решение не сходится, при γ = 0,9 сходится. Программа сообщает «Не сошлось» вместо зависания.');
}

// ---------- 8. Робот ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Робот', 'Идёт по стрелкам, ветер сносит случайно; журнал — строка на шаг');
  fit(s, shot('gui_6_robot.png'), 0.6, 1.8, 5.9, 4.3);
  fit(s, shot('gui_7_loss.png'), 6.8, 1.8, 5.9, 4.3);
  s.addText('Дошёл до +1 за 9 шагов, дважды снесло', { x: 0.6, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: PLUS, bold: true, margin: 0, isTextBox: true });
  s.addText('Не повезло: в (3,3) дважды вниз, из (3,2) вправо — в −1', { x: 6.8, y: 6.2, w: 5.9, h: 0.7, fontFace: BODY, fontSize: 15, color: MINUS, margin: 0, isTextBox: true });
  s.addNotes('Оптимальная стратегия не гарантирует победу — она даёт наибольшую среднюю сумму. Случайное число r: меньше 0,8 — вперёд, до 0,9 — влево, иначе вправо.');
}

// ---------- 8а. Лабиринт ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Большое поле: лабиринт 10×10', 'Стены змейкой и три ловушки; R = −0,01, потому что путь около 60 шагов');
  fit(s, shot('gui_14_maze_robot.png'), 0.6, 1.8, 7.6, 5.3);
  const stats = [
    ['187', 'итераций по значениям', ARROW],
    ['10', 'итераций по стратегиям — ответ тот же', ARROW],
    ['≈ 10 %', 'путей в −1 по 2000 запускам', MINUS],
  ];
  stats.forEach(([v, l, col], i) => {
    s.addText(v, { x: 8.6, y: 1.9 + i * 1.6, w: 4.1, h: 0.8, fontFace: HEAD, fontSize: 38, bold: true, color: col, margin: 0, isTextBox: true });
    s.addText(l, { x: 8.6, y: 2.7 + i * 1.6, w: 4.1, h: 0.7, fontFace: BODY, fontSize: 15, color: C.muted, margin: 0, isTextBox: true });
  });
  s.addNotes('При R = −0,04 путь стоит дороже выигрыша (U старта −1,07), поэтому плата меньше. У ловушек (6,2) и (9,5) робот стоит спиной к ним и упирается в край и в стену — назад ветер не сносит.');
}

// ---------- 9. Архитектура ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Архитектура', 'Модель — алгоритмы — робот — управление — окно; всё кроме окна не зависит от JavaFX');
  const cols = [
    ['model', 'GridWorld — поле, правка\nSettings — R(s), γ, p\nMdp — исходы хода:\n0,8 / 0,1 / 0,1, стена', PLUS, C.pathBg],
    ['solver + robot', 'Solver — интерфейс\nValueIteration, PolicyIteration\nBellman — среднее, лучший ход\nRobot — путь со сносом', ARROW, C.waterBg],
    ['controller + view', 'AppController — кнопки, таймер\nSettingsForm, GridForm — ввод\nMdpApp, GridCanvas — окно\nFormDialog — новое поле', C.cheese, C.cheeseBg],
  ];
  cols.forEach(([name, text, col, bg], i) => {
    const x = 0.6 + i * 4.1;
    card(s, x, 1.9, 3.8, 2.95, bg);
    s.addText(name, { x: x + 0.3, y: 2.1, w: 3.3, h: 0.5, fontFace: HEAD, fontSize: 22, bold: true, color: col, margin: 0, isTextBox: true });
    s.addText(text, { x: x + 0.3, y: 2.75, w: 3.3, h: 2.3, fontFace: BODY, fontSize: 15, color: C.ink, margin: 0, paraSpaceAfter: 6, valign: 'top', isTextBox: true });
  });
  s.addText([
    { text: 'Полиморфизм: ', options: { bold: true } }, { text: 'два алгоритма за одним интерфейсом Solver, выбор через перечисление Algorithm.', options: { breakLine: true } },
    { text: 'Инкапсуляция: ', options: { bold: true } }, { text: 'поле неизменяемо — правка даёт новое поле; вероятности сноса скрыты в Mdp.outcomes.' },
  ], { x: 0.6, y: 5.2, w: 12.1, h: 1.3, fontFace: BODY, fontSize: 15, color: C.ink, paraSpaceAfter: 8, margin: 0, valign: 'top', isTextBox: true });
  s.addNotes('Алгоритмы различаются одной строкой: по значениям ход выбирается лучший, по стратегиям — берётся из стрелки. Логика кнопок проверяется тестами без окна.');
}

// ---------- 10. UML ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'UML-диаграммы', 'Шесть видов диаграмм, построены в PlantUML');
  const list = [
    ['usecase.png', 'Прецеденты'], ['activity_policy.png', 'Деятельность'], ['class_model.png', 'Классы'],
    ['sequence_iterate.png', 'Взаимодействие'], ['state_solver.png', 'Состояния'], ['component.png', 'Компоненты'],
  ];
  list.forEach(([file, name], i) => {
    const col = i % 3, row = Math.floor(i / 3);
    const x = 0.6 + col * 4.1, y = 1.8 + row * 2.7;
    card(s, x, y, 3.8, 2.45, C.light);
    fit(s, uml(file), x + 0.15, y + 0.12, 3.5, 1.8);
    s.addText(name, { x: x + 0.15, y: y + 1.98, w: 3.5, h: 0.38, fontFace: BODY, fontSize: 15, bold: true, color: C.ink, align: 'center', margin: 0, isTextBox: true });
  });
  s.addNotes('В отчёте десять диаграмм: прецеденты; деятельность — оба алгоритма; классы — модель и алгоритмы, робот и окно; последовательность — «Итерация» и шаг робота; состояния — решение и робот; компоненты.');
}

// ---------- 11. Итоги ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Итоги', { x: 0.6, y: 0.6, w: 12, h: 0.9, fontFace: HEAD, fontSize: 40, bold: true, color: C.white, margin: 0, isTextBox: true });
  const res = [
    ['Два алгоритма', 'итерация по значениям и по стратегиям — таблица и стрелки задания'],
    ['Ход решения', 'по итерациям; для стратегий — отдельно оценка и улучшение'],
    ['Эксперименты', 'своё поле до 10×10, R(s), γ, вероятность; робот со сносом'],
    ['ООП, UML, тесты', 'шесть пакетов; шесть видов UML; 141 автотест'],
  ];
  res.forEach(([name, text], i) => {
    const y = 1.9 + i * 1.2;
    badge(s, 0.6, y, 0.6, PLUS, String(i + 1), C.white, 18);
    s.addText(name, { x: 1.45, y: y - 0.02, w: 2.6, h: 0.6, fontFace: BODY, fontSize: 22, bold: true, color: C.white, margin: 0, valign: 'middle', isTextBox: true });
    s.addText(text, { x: 4.1, y: y - 0.02, w: 8.6, h: 0.6, fontFace: BODY, fontSize: 18, color: 'D3D1C7', margin: 0, valign: 'middle', isTextBox: true });
  });
  s.addText('Спасибо за внимание', { x: 0.6, y: 6.6, w: 12, h: 0.5, fontFace: BODY, fontSize: 18, color: 'B4B2A9', margin: 0, isTextBox: true });
  s.addNotes('Главное: стратегия максимизирует ожидаемую полезность; среднее по путям робота совпадает с U старта.');
}

pres.writeFile({ fileName: OUT }).then(f => console.log('готово:', f));
