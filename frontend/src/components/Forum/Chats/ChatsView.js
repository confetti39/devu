import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import './chatsView.css';
import Submenu from '../Submenu';
import ab from '../../../img/a.png';
import hit from '../../../img/hit.png';
import share from '../../../img/share.png';
import warning from '../../../img/warning.png';
import like from '../../../img/like.png';
import like_color from '../../../img/like_color.png';
import more from '../../../img/more.png';
import FooterGray from '../../Home/FooterGray';

const ChatsView = () => {
  let now = new Date();
  let seconds = now.getSeconds();
  let hours = now.getHours() - 9;
  let minutes = now.getMinutes();
  let year = now.getFullYear();
  let month = now.getMonth() + 1;
  let date = now.getDate();

  const navigate = useNavigate();
  const location = useLocation();
  const [postData, setPostData] = useState([]);
  const [isLike, setLike] = useState(false);
  const username = localStorage.getItem('username');
  const [comment, setComment] = useState('');
  const [modifycomment, setModifyComment] = useState('');
  const [likePosts, setLikePosts] = useState([]);

  const [showDropdownContent, setShowDropdownContent] = useState(0);
  const [showModifyContent, setShowModifyContent] = useState(0);
  const onChangeComment = (e) => {
    setComment(e.target.value);
  };
  const onChangeModifyComment = (e) => {
    setModifyComment(e.target.value);
  };

  let pathname = location.pathname;
  let [a, b, postId] = pathname.split('/');
  var comment_num;
  // useLocation으로 pathname을 추출한 후, '/'를 기준으로 parameter를 분리함

  useEffect(() => {
    fetchData();
    window.scrollTo(0, 0);
    fetchLikeData();
  }, []);

  useEffect(() => {
    console.log("useEffect");
    fetchLikeData();
    handleGetLike();
  }, [isLike]);

  const fetchData = async () => {
    const res = await axios.get(
      process.env.REACT_APP_DB_HOST + `/community/chats/${postId}`
    );
    // console.log(res.data);
    const _postData = {
      id: res.data.id,
      title: res.data.title,
      content: res.data.content,
      hit: res.data.hit,
      like: res.data.like,
      username: res.data.username,
      date: res.data.createAt.substr(0, 10),
      hours: Number(res.data.createAt.substr(11, 2)) + 9,
      minutes: Number(res.data.createAt.substr(14, 2)),
      seconds: Number(res.data.createAt.substr(17, 2)),
      tags: res.data.tags,
      studyStatus: res.data.studyStatus,
      comments: res.data.comments,
    };
    setPostData(_postData);
    comment_num = res.data.comments.length;
  };

  const fetchLikeData = async () => {
    await axios
      .get(process.env.REACT_APP_DB_HOST + `/api/myLikes`)
      .then((res) => {
        const _likePosts = res.data.map((rowData) => rowData.id);
        setLikePosts(_likePosts);
      })
      .catch((err) => console.log(err));
  };

  const handlePostLike = async () => {
    const data = {
      username: username,
      postId: postData.id,
    };
    await axios
      .post(process.env.REACT_APP_DB_HOST + `/api/like`, JSON.stringify(data), {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .then((res) => {
        console.log('res.data', res.data.liked);
        if (res.data.liked) setLike(like + 1);
        else setLike(like - 1);
      })
      .catch((res) => {
        console.log(res);
      });
  };

  const handleGetLike = async () => {
    axios.get(process.env.REACT_APP_DB_HOST + `/api/like`,
      {
        params: {
          postId: postId,
        },
      }
    ).then((res) => {
      setPostData({
        ...postData,
        like: res.data.likeSize,
      });
    }).catch((err) => console.log(err));
  }

  const handlePostDelete = async () => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      await axios
        .delete(process.env.REACT_APP_DB_HOST + `/community/chat/${postId}`)
        .then(() => {
          console.log('삭제 성공!');
          navigate(-1);
        })
        .catch((res) => console.log(res));
    } else {
      alert('취소하였습니다!');
    }
  };

  const handleCommentDelete = async (id) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      await axios
        .delete(process.env.REACT_APP_DB_HOST + `/api/comments/${id}`)
        .then(() => {
          console.log('삭제 성공!');
          navigate(0);
        })
        .catch((res) => console.log(res));
    } else {
      alert('취소하였습니다!');
    }
  };

  const handleCommentModify = async (id) => {
    if (modifycomment !== '') {
      const data = {
        contents: modifycomment,
      };

      await axios
        .patch(
          process.env.REACT_APP_DB_HOST + `/api/comments/${id}`,
          JSON.stringify(data),
          {
            headers: {
              'Content-Type': 'application/json',
              Authorization: `${localStorage.getItem('accessToken')}`,
              // 'X-AUTH-ACCESS-TOKEN': `${localStorage.getItem('accessToken')}`,
            },
          }
        )
        .then(() => {
          navigate(0);
        })
        .catch((res) => {
          console.log(res);
        });
    } else {
      alert('댓글을 작성해주세요!');
    }
  };

  const handleComment = async () => {
    if (comment !== '') {
      const data = {
        username: username,
        postId: postId,
        contents: comment,
        // parent: parent,
        // group: group,
      };

      await axios
        .post(
          process.env.REACT_APP_DB_HOST + `/api/comments`,
          JSON.stringify(data),
          {
            headers: {
              'Content-Type': 'application/json',
            },
          }
        )
        .then((res) => {
          console.log(res);
          navigate(0);
        })
        .catch((res) => {
          console.log(res);
        });
    } else {
      alert('댓글을 작성해주세요!');
    }
  };

  return (
    <div>
      <Submenu />
      <div>
        {postData ? (
          <div className="chats-view">
            <div className="chats-detail-top">
              <div className="chats-contents-all">
                <div className="chats-detail-top">
                  <div className="chats-profile">
                    <img className="chats-photo" src={ab} alt="" />
                  </div>
                  <div className="chats-owner">{postData.username}</div>
                  <div className="chats-date">
                    {postData.date} {postData.hours}:{postData.minutes}:
                    {postData.seconds}
                  </div>
                </div>
                <div className="chats-top">
                  <div className="chats-title">{postData.title}</div>
                </div>
                <div className="chats-content">{postData.content}</div>
              </div>
              <div className="chats-sidebar">
                <div className="chats-sidebar-item">
                  <img className="img-detail-hit" src={hit} alt="" />
                  <h8 className="detail-sidebar-text">{postData.hit}</h8>
                </div>
                <div className="chats-sidebar-btn" onClick={() => handlePostLike()}>
                  <button className="detail-sidebar-btn">
                    {likePosts.includes(postData.id) ? (
                      <img
                        className="img-detail-like"
                        src={like_color}
                        alt=""
                      />
                    ) : (
                      <img className="img-detail-like" src={like} alt="" />
                    )}
                    {postData.like}
                  </button>
                </div>
                <div className="chats-sidebar-btn">
                  <img className="img-detail-like" src={share} alt="" />
                  <button className="detail-sidebar-btn">공유</button>
                </div>
                <div className="chats-sidebar-btn">
                  <img className="img-detail-like" src={warning} alt="" />
                  <button className="detail-sidebar-btn">신고</button>
                </div>
              </div>
            </div>
            <div className="chats-content-bottom">
              <div className="chats-tags">
                {postData.tags &&
                  postData.tags.map((tag) => (
                    <div className="chats-tag">{tag}</div>
                  ))}
              </div>
              {postData.username === username ? (
                <div className="chats-btns">
                  <Link
                    className="btn-modify"
                    to={`/chatsDetail/${postId}/modify`}
                  >
                    수정
                  </Link>
                  <button
                    className="btn-delete-post"
                    onClick={() => {
                      handlePostDelete();
                    }}
                  >
                    삭제
                  </button>
                </div>
              ) : null}
            </div>
            <div className="chats-detail-bottom">
              <div className="chats-write-comments">
                <input
                  className="comment"
                  id="comment"
                  name="comment"
                  value={comment}
                  onChange={(e) => onChangeComment(e)}
                  placeholder="댓글을 달아주세요."
                />
                <button
                  className="btn-comment"
                  onClick={() => {
                    handleComment();
                  }}
                >
                  댓글달기
                </button>
              </div>
              {postData.comments ? (
                <div className="chats-comments-all">
                  <div className="number-comments"></div>
                  <div className="chats-comments">
                    {postData.comments &&
                      postData.comments.map((comment) => (
                        <div className="container-comments">
                          <div className="comment-detail">
                            <div className="comments-top">
                              <div>
                                <img
                                  className="comment-photo"
                                  src={ab}
                                  alt=""
                                />
                              </div>
                              <div className="comment-top">
                                <div className="comment-owner">
                                  {comment.username}
                                </div>
                                {comment.username === username &&
                                  comment.commentId !== showModifyContent ? (
                                  <button className="btn-more">
                                    <img
                                      className="img-more"
                                      alt=""
                                      src={more}
                                      onClick={() => {
                                        console.log(comment.commentId);
                                        if (
                                          showDropdownContent ===
                                          comment.commentId
                                        )
                                          setShowDropdownContent(0);
                                        else
                                          setShowDropdownContent(
                                            comment.commentId
                                          );
                                      }}
                                    />
                                    {comment.commentId ===
                                      showDropdownContent ? (
                                      <ul className="more-submenu">
                                        <button
                                          onClick={() => {
                                            setShowModifyContent(
                                              comment.commentId
                                            );
                                            setShowDropdownContent(0);
                                          }}
                                        >
                                          수정
                                        </button>
                                        <button
                                          onClick={() => {
                                            handleCommentDelete(
                                              comment.commentId
                                            );
                                          }}
                                        >
                                          삭제
                                        </button>
                                      </ul>
                                    ) : null}
                                  </button>
                                ) : null}
                              </div>
                            </div>
                            {comment.commentId === showModifyContent ? (
                              <div className="container-modify-comments">
                                <input
                                  className="comment"
                                  id="comment"
                                  name="comment"
                                  defaultValue={comment.contents}
                                  onChange={(e) => onChangeModifyComment(e)}
                                />
                                <div className="btn-comments">
                                  <button
                                    className="btn-comment-sub"
                                    onClick={() => {
                                      handleCommentModify(comment.commentId);
                                    }}
                                  >
                                    수정하기
                                  </button>
                                  <button
                                    className="btn-comment-sub"
                                    onClick={() => {
                                      setShowModifyContent(0);
                                    }}
                                  >
                                    취소
                                  </button>
                                </div>
                              </div>
                            ) : (
                              <div className="comment-content">
                                {comment.contents}
                              </div>
                            )}

                            <div className="comment-date">
                              {comment.createAt.slice(0, 4) == year
                                ? comment.createAt.slice(5, 7) == month &&
                                  comment.createAt.slice(8, 10) == date
                                  ? comment.createAt.slice(11, 13) == hours
                                    ? comment.createAt.slice(14, 16) == minutes
                                      ? seconds -
                                      comment.createAt.slice(17, 19) +
                                      '초 전'
                                      : minutes -
                                        comment.createAt.slice(14, 16) ==
                                        1 &&
                                        seconds < comment.createAt.slice(17, 19)
                                        ? 60 -
                                        comment.createAt.slice(17, 19) +
                                        seconds +
                                        '초 전'
                                        : minutes -
                                        comment.createAt.slice(14, 16) +
                                        '분 전'
                                    : hours -
                                    comment.createAt.slice(11, 13) +
                                    '시간 전'
                                  : comment.createAt.slice(5, 7) +
                                  '.' +
                                  comment.createAt.slice(8, 10)
                                : comment.createAt.slice(2, 4) +
                                '.' +
                                comment.createAt.slice(5, 7) +
                                '.' +
                                comment.createAt.slice(8, 10)}
                            </div>
                          </div>
                        </div>
                      ))}
                  </div>
                </div>
              ) : null}
              <FooterGray />
            </div>
          </div>
        ) : (
          '해당 게시글을 찾을 수 없습니다.'
        )}
      </div>
    </div>
  );
};

export default ChatsView;
