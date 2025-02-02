import React from 'react'
import { Link } from 'react-router-dom'
import './registerOver.css'
import puzzle from '../../img/puzzle.png'
import FooterGray from '../Home/FooterGray'

const RegisterOver = () => {
    return (
        <div>
            <div className='registerover-all'>
                <img className='img-puzzle' src={puzzle} alt="" />
                <h1 className='text-main'>데뷰 회원가입이 완료되었습니다.</h1>
                <h2 className='text-sub'>지금 바로 다양한 서비스를 확인해보세요</h2>
                <div className='registerover-btns'>
                    <Link className='btn-to-main' to="/main">메인으로</Link>
                </div>
            </div>
            <FooterGray />
        </div>
    )
}

export default RegisterOver