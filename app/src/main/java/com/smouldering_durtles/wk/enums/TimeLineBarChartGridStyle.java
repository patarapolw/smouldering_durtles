/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.enums;

/**
 * The style of the horizontal grid lines and Y-axis labels on the timeline bar chart.
 */
public enum TimeLineBarChartGridStyle {
    /**
     * No horizontal grid lines, no Y-axis labels.
     */
    OFF,

    /**
     * Horizontal grid lines and Y-axis labels are scaled for bars.
     */
    FOR_BARS,

    /**
     * Horizontal grid lines and Y-axis labels are scaled for the waterfall line.
     */
    FOR_WATERFALL
}
