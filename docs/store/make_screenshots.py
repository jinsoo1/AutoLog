#!/usr/bin/env python3
"""Play 스토어 등록정보용 스크린샷 생성.

사용법:
  1. 기기 스크린샷을 docs/store/raw/ 에 넣는다 (shot_1 ~ shot_N, png·jpg 아무거나)
     — 이름의 숫자가 스토어 노출 순서다. shot_1 이 첫 장.
  2. 아래 SLIDES 의 (파일, 제목, 부제)를 원하는 대로 수정
  3. python3 docs/store/make_screenshots.py
  → docs/store/screenshots/screenshot_NN.png (1080×1920)

폰트는 앱 리소스의 Pretendard OTF를, 배경 장식은 앱 아이콘의 게이지 아치를 그대로 쓴다.
헤드리스 크롬이 필요하다(macOS 기본 경로).
"""
import base64
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
STORE = os.path.join(ROOT, "docs", "store")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# (raw 파일 이름, 제목(\n으로 줄바꿈), 부제) — 스토어 노출 순서
# shot_N = N번째 슬라이드. 캡처 파일 이름을 순서대로만 맞추면 된다.
SLIDES = [
    ("shot_1", "지금 필요한 정비를\n한눈에", "교체 시기가 가까운 항목부터 먼저 보여드려요"),
    ("shot_2", "교체 주기는\n앱이 계산해요", "주행거리와 기간 중 먼저 오는 쪽으로 알려드려요"),
    ("shot_3", "모든 정비 기록을\n하나의 타임라인으로", "일회성 수리도 배지로 구분돼요"),
    ("shot_4", "앱을 열지 않아도\n다음 정비 확인", "홈 화면 위젯에서 남은 거리와 날짜가 보여요"),
    ("shot_5", "주유와 충전을\n한 화면에", "월 지출 그래프로 유지비가 보여요"),
    ("shot_6", "이 소리, 어디를\n점검해야 할까", "증상을 고르면 점검 항목과 주행위험도를 알려드려요"),
    ("shot_7", "회원가입 없이\n내 폰에만", "백업 파일과 엑셀로 기록을 오래 관리하세요"),
]

DECOR = """<svg style="position:absolute;right:-160px;top:-140px;width:620px;height:620px;opacity:.10" viewBox="0 0 108 108">
  <path d="M29.85,70.69 A25.7,25.7 0 1 1 78.15,70.69" fill="none" stroke="#DBEAFE" stroke-width="6" stroke-linecap="round"/>
</svg>
<svg style="position:absolute;left:-200px;bottom:-230px;width:760px;height:760px;opacity:.07" viewBox="0 0 108 108">
  <path d="M29.85,70.69 A25.7,25.7 0 1 1 78.15,70.69" fill="none" stroke="#38BDF8" stroke-width="6" stroke-linecap="round"/>
</svg>"""

TPL = """<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{{font-family:'P';font-weight:400;src:url(data:font/otf;base64,{REG}) format('opentype')}}
@font-face{{font-family:'P';font-weight:600;src:url(data:font/otf;base64,{SEMI}) format('opentype')}}
@font-face{{font-family:'P';font-weight:800;src:url(data:font/otf;base64,{BOLD}) format('opentype')}}
*{{margin:0;box-sizing:border-box}}
body{{width:1080px;height:1920px;overflow:hidden;position:relative;
  background:linear-gradient(135deg,#1E3A8A 0%,#152C6B 100%);
  font-family:'P',sans-serif;-webkit-font-smoothing:antialiased}}
.cap{{position:absolute;top:104px;left:0;right:0;text-align:center;z-index:2}}
.t{{font-size:72px;font-weight:800;color:#fff;line-height:1.24;letter-spacing:-.02em;white-space:pre-line}}
.s{{margin-top:24px;font-size:36px;font-weight:600;color:#93C5FD;letter-spacing:-.01em}}
.phone{{position:absolute;left:50%;transform:translateX(-50%);top:396px;height:1444px;
  background:#0B1220;border-radius:58px;padding:13px;z-index:2;
  box-shadow:0 44px 90px rgba(0,0,0,.45), 0 0 0 1px rgba(255,255,255,.07)}}
.phone img{{display:block;height:100%;width:auto;border-radius:46px}}
</style></head><body>
{DECOR}
<div class="cap"><div class="t">{TITLE}</div><div class="s">{SUB}</div></div>
<div class="phone"><img src="data:{MIME};base64,{IMG}"></div>
</body></html>"""


# 안드로이드 기본 스크린샷은 PNG 다. 확장자에 맞는 MIME 를 써야 크롬이 확실히 렌더한다.
EXT_MIME = {".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".webp": "image/webp"}


def find_raw(shot: str):
    """raw/ 에서 shot 이름의 파일을 확장자 상관없이 찾는다. (파일, MIME) 또는 (None, None)."""
    for ext, mime in EXT_MIME.items():
        path = os.path.join(STORE, "raw", f"{shot}{ext}")
        if os.path.exists(path):
            return path, mime
    return None, None


def b64(path: str) -> str:
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode()


def main() -> int:
    fonts = {k: b64(os.path.join(FONT_DIR, f"pretendard_{n}.otf"))
             for k, n in (("REG", "regular"), ("SEMI", "semibold"), ("BOLD", "bold"))}

    out_dir = os.path.join(STORE, "screenshots")
    tmp_dir = os.path.join(STORE, "_slides")     # 중간 산출물(커밋 금지)
    os.makedirs(out_dir, exist_ok=True)
    os.makedirs(tmp_dir, exist_ok=True)

    for i, (shot, title, sub) in enumerate(SLIDES, 1):
        raw, mime = find_raw(shot)
        if raw is None:
            print(f"skip {i}: docs/store/raw/{shot}.(png|jpg) 없음"); continue
        html_path = os.path.join(tmp_dir, f"slide_{i}.html")
        with open(html_path, "w", encoding="utf-8") as f:
            f.write(TPL.format(**fonts, DECOR=DECOR,
                               TITLE=title, SUB=sub, MIME=mime, IMG=b64(raw)))
        png = os.path.join(out_dir, f"screenshot_{i:02d}.png")
        subprocess.run([CHROME, "--headless", "--disable-gpu", "--hide-scrollbars",
                        "--window-size=1080,1920", f"--screenshot={png}",
                        f"file://{html_path}"],
                       check=True, capture_output=True)
        print(f"{png} 생성")
    return 0


if __name__ == "__main__":
    sys.exit(main())
