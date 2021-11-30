/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.aws.iot.edgeconnectorforkvs.videouploader.callback;

import com.aws.iot.edgeconnectorforkvs.util.VideoRecordVisitor;
import com.aws.iot.edgeconnectorforkvs.videouploader.model.VideoFile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * This callback wrap more information while doing uploading so that callee can get information form it.
 */
@Slf4j
public abstract class UploadCallBack implements Runnable {

    private final List<Long> persistedFragmentTimecodes = new ArrayList<>();

    private final Date dateBegin;

    private final UpdateFragmentStatus updateHandler;

    @Setter
    private List<VideoFile> videoFiles = null;

    @Getter
    private VideoFile uploadingFile = null;

    @Getter
    private VideoFile uploadedFile = null;

    /**
     * Constructor.
     *
     * @param date          Date of the request. It might change when doing historical file
     * @param updateHandler A handler to get updates from persisted fragment or uploaded file
     */
    public UploadCallBack(Date date, UpdateFragmentStatus updateHandler) {
        this.dateBegin = new Date(date.getTime());
        this.updateHandler = updateHandler;
    }

    /**
     * Add a newly persisted fragment.
     *
     * @param timecodeMs Timecode of the newly persisted fragment.
     */
    public void addPersistedFragmentTimecode(long timecodeMs) {
        long absoluteTimecodeMs = dateBegin.getTime() + timecodeMs;
        persistedFragmentTimecodes.add(absoluteTimecodeMs);

        if (videoFiles != null) {
            updateVideoFileStatus(absoluteTimecodeMs);
        }
        update(absoluteTimecodeMs, uploadedFile);
    }

    private void update(long absoluteTimecodeMs, VideoFile uploadedFile) {
        if (updateHandler != null) {
            updateHandler.update(absoluteTimecodeMs, uploadedFile);
        }
    }

    private void updateVideoFileStatus(Long timecodeMs) {
        boolean isFileUploaded = false;
        ListIterator<VideoFile> videoFileIter = videoFiles.listIterator(videoFiles.size());
        while (videoFileIter.hasPrevious()) {
            VideoFile videoFile = videoFileIter.previous();
            if (videoFile.getVideoDate().before(new Date(timecodeMs))) {
                if (uploadingFile != null && uploadingFile != videoFile) {
                    /* When we upload different file, it means the previous one has been uploaded. */
                    isFileUploaded = true;
                    uploadedFile = uploadingFile;
                    uploadedFile.setUploaded(true);
                    VideoRecordVisitor.markVideoAsUploaded(uploadedFile);
                }
                uploadingFile = videoFile;

                if (isFileUploaded) {
                    /* Check if any earlier video hasn't been marked as uploaded. It happened when we merge a video
                     * file that doesn't have any valid fragment. For example, there are videos to be uploaded:
                     *      video_100.mkv,
                     *      video_200.mkv,
                     *      video_300.mkv.
                     * We may get ACK of fragment timecode 201 and 301.  In this case, we need a forward search to
                     * mark video_100.mkv as an uploaded one.
                     * */
                    while (videoFileIter.hasPrevious()) {
                        VideoFile earlierVideoFile = videoFileIter.previous();
                        if (!earlierVideoFile.isUploaded()) {
                            earlierVideoFile.setUploaded(true);
                            VideoRecordVisitor.markVideoAsUploaded(earlierVideoFile);
                            update(timecodeMs, earlierVideoFile);
                        }
                    }
                }

                break;
            }
        }
    }

    /**
     * It's called when uploading complete without error. It'll mark all video files as uploaded.
     */
    public void onComplete() {
        if (videoFiles != null) {
            ListIterator<VideoFile> videoFileIter = videoFiles.listIterator(videoFiles.size());
            while (videoFileIter.hasPrevious()) {
                VideoFile videoFile = videoFileIter.previous();
                if (videoFile.isUploaded()) {
                    break;
                }
                videoFile.setUploaded(true);
                VideoRecordVisitor.markVideoAsUploaded(videoFile);
            }
        }
    }

    /**
     * Update timeBegin while doing historical uploading.
     *
     * @param newDate The new date
     */
    public void setDateBegin(Date newDate) {
        dateBegin.setTime(newDate.getTime());
    }

    /**
     * Get persisted fragment timecodes.
     *
     * @return The list of persisted fragment timecodes
     */
    public List<Long> getPersistedFragmentTimecodes() {
        return new ArrayList<>(persistedFragmentTimecodes);
    }

    /**
     * Get uploaded files.
     *
     * @return The list of uploaded files
     */
    public List<VideoFile> getVideoFiles() {
        return new ArrayList<>(videoFiles);
    }
}
