package com.graduationdesign.newsrecommendation.service;

import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.vo.ProfileCommentVO;
import com.graduationdesign.newsrecommendation.vo.ProfileNewsItemVO;
import com.graduationdesign.newsrecommendation.vo.ProfileSummaryVO;

public interface ProfileService {

    ProfileSummaryVO getProfileSummary(User currentUser);

    PageResult<ProfileNewsItemVO> pageFavorites(Long userId, long page, long size);

    PageResult<ProfileNewsItemVO> pageLikes(Long userId, long page, long size);

    PageResult<ProfileNewsItemVO> pageHistory(Long userId, long page, long size);

    PageResult<ProfileCommentVO> pageComments(Long userId, long page, long size);
}
