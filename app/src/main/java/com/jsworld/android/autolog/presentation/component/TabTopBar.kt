package com.jsworld.android.autolog.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 탭 화면 공통 상단 바.
 *
 * 탭마다 헤더를 따로 짜면 **오른쪽에 무엇을 놓았는지에 따라 높이가 달라진다** —
 * 아이콘 버튼(48dp 터치 영역)이 있는 홈은 58dp, 칩만 있는 주유·리포트는 45dp,
 * TopAppBar 를 쓰는 설정은 64dp 였다. 탭을 옮길 때마다 첫 카드가 위아래로 튀어서
 * 화면이 어긋나 보인다. 높이를 고정해 그 흔들림을 없앤다.
 *
 * 오른쪽 여백을 4dp 로 둔 건 [actions] 에 오는 아이콘 버튼이 자체 여백을 갖기
 * 때문이다. 라벨형 버튼은 스스로 좌우 여백을 준다.
 */
val TabTopBarHeight = 56.dp

/**
 * 상단 바와 첫 콘텐츠 사이 간격.
 * 탭마다 다르면(홈 6 / 주유 4 / 리포트 6 / 설정 8) 첫 카드 위치가 어긋난다.
 */
val TabContentTopPadding = 8.dp

@Composable
fun TabTopBar(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    leading: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(TabTopBarHeight)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        leading()
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        actions()
    }
}
